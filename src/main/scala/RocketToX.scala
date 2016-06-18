package RocketToX
import Chisel._

// See SCRFile in Uncore for usage example

// From Rocket
// io.smi.req.valid       --- Rocket wants to read or write
// io.smi.req.bits.rw     --- Rocket wants to read (false) or write (true)
// io.smi.req.bits.addr   --- Memory location Rocket wants to access
// io.smi.req.bits.data   --- Data Rocket wants to write (X should ignore on read)
// io.smi.resp.ready      --- Rocket ready to take back response

// From X
// io.smi.req.ready       --- X capable of taking requests 
// io.smi.resp.valid      --- X did something (write to memory successful; data to Rocket valid)
// io.smi.resp.bits       --- X data to be sent to Rocket

// Pulled in SmiIO so I don't need all of Junctions

class XSmiReq(val dataWidth: Int, val addrWidth: Int) extends Bundle {
  val rw = Bool()
  val addr = UInt(width = addrWidth)
  val data = Bits(width = dataWidth)

  override def cloneType =
    new XSmiReq(dataWidth, addrWidth).asInstanceOf[this.type]
}

class XSmiIO(val dataWidth: Int, val addrWidth: Int) extends Bundle {
  // Rocket -> FFT
  val req = Decoupled(new XSmiReq(dataWidth, addrWidth))
  // FFT -> Rocket
  val resp = Decoupled(Bits(width = dataWidth)).flip

  override def cloneType =
    new XSmiIO(dataWidth, addrWidth).asInstanceOf[this.type]
}

class RocketToX(dataWidth:Int, addrWidth:Int) extends Module {

  // Delay expected from rAddr @ memory to valid data out (can be generalized)
  // Let's choose something "large", to fake some additional process needing to occur in between...
  val dly = 5

  require(dly > 0, "Delay should >0")

  // Interface memory depth (Note: Rocket is not streaming)
  val depth = math.pow(2,addrWidth).toInt

  val io = new Bundle {
    val smi = new XSmiIO(dataWidth,addrWidth).flip
  }

  val mem = SeqMem(depth,Bits(width=dataWidth))
  val wAddr = io.smi.req.bits.addr

  // Write when Rocket out is valid & in write mode
  val we = io.smi.req.fire() & io.smi.req.bits.rw
  mem.doWrite(wAddr,we,io.smi.req.bits.data,None)
  
  // Read data (and therefore address) needs to be held until Rocket acknowledges receipt
  val rAddr = RegInit(UInt(0,width = addrWidth))
  when (io.smi.req.fire()){
    rAddr := io.smi.req.bits.addr
  }

  // 3 main X response states
  // Idle: req.ready & !resp.valid --> (on req.fire = X received a request from Rocket)
  // Processing: !req.ready & !resp.valid --> (on counter = delay-1 a.k.a. X response going to be valid)
  // Complete: !req.ready & resp.valid --> (on resp.fire = Rocket received response)
  // Idle
  // Actual state machine for read, but use it for write too

  val ioIdle :: ioProcessing :: ioComplete :: Nil = Enum(UInt(),3)
  val ioState = RegInit(ioIdle)

  val dlyCount = RegInit(UInt(dly))
  when(io.smi.req.fire() & (ioState === ioIdle)){
    dlyCount := UInt(0)
  }.otherwise{
    dlyCount := dlyCount + UInt(1)
  }

  when((ioState === ioIdle) & io.smi.req.fire()){
    ioState := ioProcessing
  }.elsewhen((ioState === ioProcessing) & (dlyCount === UInt(dly-1))){
    ioState := ioComplete
  }.elsewhen((ioState === ioComplete) & io.smi.resp.fire()){
    ioState := ioIdle
  }

  io.smi.resp.valid := (ioState === ioComplete)
  io.smi.req.ready := (ioState === ioIdle)

  io.smi.resp.bits := mem.read(rAddr)

}