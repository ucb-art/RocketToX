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

Run `source enter.bash`.

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

> Note: Every time you make changes to these submodules, you should commit + push them to their respective git repos and **git add** them in the next repo up in the hierarchy.

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