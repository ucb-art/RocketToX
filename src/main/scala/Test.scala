package RocketToX
import Chisel._

class RocketToXTests(c: RocketToX) extends Tester(c) {

  for (i <- 0 until c.depth){
    write(d = i+1, a = i)
    if (read(a = i) != i+1) throwException("The following address was incorrect: " + i)
    else println("Address " + i + " is correct!")
  }

  def write(d:BigInt,a:BigInt): Unit ={
    var xready = peek(c.io.smi.req.ready)
    while(xready == 0){
      // X not ready
      step(1)
      xready = peek(c.io.smi.req.ready)
    }
    poke(c.io.smi.req.valid,true)
    poke(c.io.smi.req.bits.rw,true)
    poke(c.io.smi.req.bits.addr,a)
    poke(c.io.smi.req.bits.data,d)
    poke(c.io.smi.resp.ready,false)
    step(1)
    poke(c.io.smi.req.valid,false)
    poke(c.io.smi.resp.ready,true)
    var xvalid = peek(c.io.smi.resp.valid)
    while(xvalid == 0){
      // X not valid
      step(1)
      xvalid = peek(c.io.smi.resp.valid)
    }
  }

  def read(a:BigInt): BigInt ={
    var xready = peek(c.io.smi.req.ready)
    while(xready == 0){
      // X not ready
      step(1)
      xready = peek(c.io.smi.req.ready)
    }
    poke(c.io.smi.req.valid,true)
    poke(c.io.smi.req.bits.rw,false)
    poke(c.io.smi.req.bits.addr,a)
    poke(c.io.smi.resp.ready,false)
    step(1)
    poke(c.io.smi.req.valid,false)
    poke(c.io.smi.resp.ready,true)
    var xvalid = peek(c.io.smi.resp.valid)
    while(xvalid == 0){
      // X not valid
      step(1)
      xvalid = peek(c.io.smi.resp.valid)
    }
    peek(c.io.smi.resp.bits)
  }

}