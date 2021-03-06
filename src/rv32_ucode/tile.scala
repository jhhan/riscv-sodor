//**************************************************************************
// RISCV Processor Tile
//--------------------------------------------------------------------------
//

package Sodor
{

import chisel3._
import chisel3.util._

import Constants._
import Common._   
import Common.Util._   


class SodorTileIo extends Bundle  
{
   val host     = new HTIFIO()
}

class SodorTile(implicit val conf: SodorConfiguration) extends Module
{
   val io = IO(new SodorTileIo())
   
   val core   = Module(new Core(resetSignal = io.host.reset))
   val memory = Module(new ScratchPadMemory(num_core_ports = 1))

   core.io.mem <> memory.io.core_ports(0)

   // HTIF/memory request
   memory.io.htif_port.req.valid     := io.host.mem_req.valid
   memory.io.htif_port.req.bits.addr := io.host.mem_req.bits.addr.toUInt
   memory.io.htif_port.req.bits.data := io.host.mem_req.bits.data
   memory.io.htif_port.req.bits.fcn  := Mux(io.host.mem_req.bits.rw, M_XWR, M_XRD)
   io.host.mem_req.ready             := memory.io.htif_port.req.ready     

   // HTIF/memory response
   io.host.mem_rep.valid := memory.io.htif_port.resp.valid
   io.host.mem_rep.bits := memory.io.htif_port.resp.bits.data

   core.io.host <> io.host
}
 
}
