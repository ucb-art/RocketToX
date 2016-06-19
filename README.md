Testing Accelerator X with Rocket Chip
===================

Never waste weeks building testing interfaces for your digital chips again!

> Build a barebones interface in hardware. Anything complicated (control logic included) can/*should* be tested in software to make your life easier. Also, ideally, you should have tested your accelerator standalone prior to this setup, just to decouple possible sources of errors.

----------

Getting Started with Rocket Chip
===================

Clone Rocket chip + get all of its submodules locally (submodule update takes some time). 

```
git clone https://github.com/ucb-bar/rocket-chip.git
cd rocket-chip
git submodule update --init --recursive
```

Any time you want to get the latest Rocket chip,  just **git pull** and run `git submodule update --init --recursive` again.

First, make sure that your version of Rocket "works".  Create an **enter.bash** script in **rocket-chip** with the following:

```
export RISCV="/*path-to-rocket-chip*/install"
export PATH="$RISCV/bin:$PATH"

if test -f /ecad/tools/vlsi.bashrc
then
  source /ecad/tools/vlsi.bashrc
fi
```

> Note: The last 4 lines are used for running VCS simulations with the Chisel-generated Verilog (i.e. in **vsim**), and **/ecad/tools/vlsi.bashrc** should be specific to the setup you're using i.e. pointing to where your VCS, etc. tools are located.  

Run `source enter.bash`. (Necessary each startup)

Then build **riscv-tools**
```
cd riscv-tools
./build.sh
```

To check that Rocket builds and also runs through its basic test suite, in the top-level directory, do the following:

```
cd emulator
make run-asm-tests
```

The above commands use Chisel's C++ simulator and don't require VCS (the last 4 lines of the bash script), but they'll take a while to run.

Let's also not forget to update your repo! Add **install/** to the **.gitignore** of your top-level directory. Then,

```
git add .gitignore
git add enter.bash
```

> Or maybe don't add *enter.bash* since it's not machine agnostic...

----------

Modifying Infrastructure to Support Your Accelerator
===================

If you need to run any tests with your accelerator (of course!),  you'll be needing to modify the **riscv-tools** and **riscv-tests** (inside of *riscv-tools*) submodules, so you should fork those into new git repos. Start by creating two empty repos on Github or similar.

In your **rocket-chip** directory, open **.gitmodules** and replace the following:

```
[submodule "riscv-tools"]
        path = riscv-tools
        url = https://github.com/riscv/riscv-tools.git
```

with

```
[submodule "riscv-tools"]
        path = riscv-tools
        url = *your-riscv-tools-git-url*
```

Then do `git add .gitmodules`. 

Go into **riscv-tools** and check where the repository is pointing to via `git remote -v`. Then change the origin + update via: 

```
git remote set-url origin *your-riscv-tools-git-url*
git checkout master
git push origin master
```

Do the same for **riscv-tests**.

In **riscv-tools**, open **.gitmodules** and update:

```
[submodule "riscv-tests"]
        path = riscv-tests
        url = *your-riscv-tests-git-url*
```

Then `git add .gitmodules` and commit + push.

Go into **riscv-tests**, and change the origin + update via:

```
git remote set-url origin *your-riscv-tests-git-url*
git checkout master
git push origin master
```

Back in **riscv-tools**, `git add riscv-tests`.  Commit + push. 
Back in **rocket-chip**, `git add riscv-tools`. Commit. Then push your updates (don't forget to change the url!)

```
git remote set-url origin *your-rocket-chip-git-url*
git push origin master
```

> **Git Reminder**
> 
> Note: Every time you make changes to these submodules, you should commit + push them to their respective git repos and **git add** them in the next repo up in the hierarchy.
>
> In your submodule directory,  double check that you're on the branch you expect (i.e. master) via `git branch`. If you aren't, you can switch to said branch via `git checkout *branchName*`. If you want to update to the latest version of said branch, just do `git pull`. 
>
> To update *branch_a* with changes in *branch_b*:
> ``` 
>  git checkout *branch_a*
>  git merge *branch_b*
> ```
>  *git checkout* always puts you in the branch you want to be updating.

----------

Adding Your Accelerator to Rocket
===================

Add your accelerator as a submodule in **rocket-chip** via `git submodule add *url* *name*`. If/when you update Accelerator X from the outside, you can get the latest version in your Accelerator X + Rocket project via:

```
cd *name*
git pull
cd ..
git add *name*
```

Then commit + push to the top-level repo as necessary. 

If your accelerator uses a different (a.k.a. custom) version of Chisel, and if it's not compatible with Chisel3, go into your top level **Makefrag** and replace

```
ifeq ($(CHISEL_VERSION),2)
CHISEL_ARGS := $(PROJECT) $(MODEL) $(CONFIG) --W0W --minimumCompatibility 3.0.0 --backend $(BACKEND) --configName $(CONFIG) --compileInitializationUnoptimized --targetDir $(generated_dir)
else
CHISEL_ARGS := --W0W --minimumCompatibility 3.0.0 --backend $(BACKEND) --configName $(CONFIG) --compileInitializationUnoptimized --targetDir $(generated_dir)
endif
```

with

```
ifeq ($(CHISEL_VERSION),2)
CHISEL_ARGS := $(PROJECT) $(MODEL) $(CONFIG) --W0W --backend $(BACKEND) --configName $(CONFIG) --compileInitializationUnoptimized --targetDir $(generated_dir)
else
$(error "Chisel 3 isn't supported")
endif
```

Also,  don't forget to change `CHISEL_VERSION ?= 3` to 2. 

Add your project directory name to the end of the line: `default_submodules = . junctions uncore hardfloat rocket groundtest context-dependent-environments`

Now go to **project/build.scala**

Add `lazy val *name* = project in file("*name*")` assuming that your project file ID (see its **build.sbt** matches the directory name). Otherwise, add:

```
  lazy val x = Project(
    id = "actual-name-of-x",
    base = file("x"),
    settings = settings
  )
```

And modify the line `lazy val rocket     = project.dependsOn(hardfloat, uncore)` to `lazy val rocket     = project.dependsOn(*name*,hardfloat, uncore)`

> Note: If you used any customized Chisel and have your Accelerator depending on it, you should definitely have it before hardfloat and uncore, due to the way SBT pulls in dependencies...

Go to **src/main/scala/RocketChip.scala**, and under `io.prci := prci.io.tiles` add (datawith = 64,  addresswidth = 11 in this example, parameterizable for what you want):

```
val shim = Module(new RocketToX(64,11))
val XSmitoTileLink = Module(new SmiIOTileLinkIOConverter(64,11))
XSmitoTileLink.io.tl <>mmioNetwork.port("int:x")
shim.io.smi <> XSmitoTileLink.io.smi
```

Don't forget to **import** your project @ the top i.e. `import X._`

Now go to **src/main/scala/Configs.scala**, and right before `new AddrMap(entries)`, add:

```
entries += AddrMapEntry("x", MemSize(16384, MemAttr(AddrMapProt.RW)))
```

"x" should be the same as in "int:x". For whatever reason, you can't put peripheral memories as the first of the address map entries. Chisel won't error out, but just don't do it, because your testbench will fail. 

> Note: SMI uses 64 bit longs, whereas **Configs.scala** maps memories in bytes. Therefore, for 2048 longs, you need to allocate 2048*8 bytes. 

Now see if your changes compile + you can run a basic test (this time using the Verilog simulator). 

```
cd vsim
make output/rv64ui-p-ld.vpd
```

> If it ever errors out saying it can't find VCS, be sure to `source enter.bash` in **rocket-chip**


During compilation, notice in the console output something like:

>  Generated Address Map
  io:int:debug 0 - fff
  io:int:bootrom 1000 - 1fff
  io:int:rtc 2000 - 2fff
  io:int:plic 40000000 - 43ffffff
  io:int:prci 44000000 - 47ffffff
  io:int:x 48000000 - 480007ff
  io:ext 60000000 - 7fffffff
  mem 80000000 - ffffffff
    Generated Configuration String

This tells you that memory associated with your **x** block starts at address 0x48000000. This will be important for writing your own tests. 

> Note: To double check that your tests passed, you can do `echo $?`, where the tests have been written to return 0 on pass. You can also check the log via `less output/rv64ui-p-ld.out`. Also, if you think your tests have definitely failed and want to quit in the middle of running them, to keep the generated ***.vpd** file, add `.PRECIOUS: output/*.vpd` to your **emulator/Makefile**

----------

Adding C Tests for Your Accelerator
===================

C tests sit in **rocket-chip/riscv-tools/riscv-tests/benchmarks**. To make a custom test for your subproject, from your top-level directory:

```
cd riscv-tools/riscv-tests/benchmarks
mkdir *name*
```

Open the **Makefile** in the **benchmarks** directory and add the name of your directory under `bmarks = \` (follow the example).

If you used the default **RocketToX** code to check out accelerator functionality, you'll see that all it does is read in to memory and read out from memory. To test that the functionality is right, inside your **benchmarks/*name*** directory, make a new file called ***name*_main.c** with:

```
#include "util.h"

int main( int argc, char* argv[] )
{
  volatile long *base = (long *)0x48000000UL;
  int a;
  for(a = 0; a < 2048; a = a + 1 ){
    base[a] = a+1;
    long val = base[a];
    if (val != a+1){
      printf("failed on val: %ld\n", val);
      return a+1;
    }
    else {
      printf("passes %ld\n",val);
    }
  }
  return 0;
}
```
        
The code's really simple. It just stores (a+1) @ location a and expects to read the same thing back. Note that I test this for all possible address ranges 0-2047. Also, notice that the **base** address corresponds with the generated address map above (otherwise, you will be writing to something else...). Finally,  notice that the program returns 0 if it passes. 

Now go back one level to the **benchmarks** directory and `cp vvadd/bmark.mk *name*/.
`. Note that all benchmark directories must have this Makefile fragment. You'll need to replace the contents of the **bmark.mk** file with the name of your benchmark. 

You might be able to do that with `sed -i 's/vvadd/template/g' bmark.mk`. Look inside the **bmark.mk** file to see that the file names make sense (i.e. ***name*_main.c**). 

Now go to **riscv-tools/riscv-tests/build** and run `make install`. This needs to be done every time you change something in **benchmarks**. 

Then go back to top level, then **src/main/scala** and open **Testing.scala**.  Add your benchmark name to the end of (after *mt-matmul*):

```
val bmarks = new BenchmarkTestSuite("basic", "$(RISCV)/riscv64-unknown-elf/share/riscv-tests/benchmarks", LinkedHashSet(
    "median", "multiply", "qsort", "towers", "vvadd", "mm", "dhrystone", "spmv", "mt-vvadd", "mt-matmul"))
```

Then go into the **vsim** directory from top and run `make output/*benchmarkName*.riscv.vpd`. Note that the name should match with what you named your benchmark. 

> Note: If your tests failed, don't fret just yet. There are sometimes weird bugs in Rocket. Assuming this patch hasn't been upstreamed (as of 6/18/16), go into **rocket-chip/junctions** and `git cherry-pick e414fa15764b2c7b59c3318e48c44a4d7ab363bf`. Then go back to the **vsim** directory and run again.

If you want to look at your waveforms to debug things, try ` dve -full64 -vpd output/*benchmarkName*.riscv.vpd`

> Don't forget to git add all of your changes!

----------

About RocketToX
===================

This just serves as a template to guide you as you interface your accelerator with Rocket. It's only a memory interface that allows you to write to a block of memory, and read back from it (takes a few cycles -- you can parameterize via **dly**+1 because of the 1 cycle it takes to register the read address, which must be held until Rocket accepts the data from X). Because you cannot expect Rocket to stream data continuously, it uses a Decoupled interface. 

**From Rocket**

 - io.smi.req.valid --- Rocket wants to read or write (and has valid control + data)
 - io.smi.req.bits.rw --- Rocket wants to read (false) or write (true)
 - io.smi.req.bits.addr --- Memory location Rocket wants to access
 - io.smi.req.bits.data --- Data Rocket wants to write (X should ignore on read)
 - io.smi.resp.ready --- Rocket ready to take back response

**From X**

 - io.smi.req.ready --- X capable of taking requests 
 - io.smi.resp.valid --- X did something (write to memory successful; data to Rocket valid)
 - io.smi.resp.bits --- X data to be sent to Rocket

When creating a new instance of the module, you can specify the depth of the memory and the bitwidths via: `Module(new RocketToX(dataWidth = 64, addrWidth = 11))`

If you check out **src/main/scala/Test.scala**, you'll see that it just emulates memory reads and writes via peeking and poking the signals in Decoupled. 