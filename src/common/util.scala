//**************************************************************************
// RISCV Processor Utility Functions
//--------------------------------------------------------------------------

package Common
{

import chisel3._
import chisel3.util._
import scala.math._
import scala.collection.mutable.ArrayBuffer

object Util
{
   implicit def intToUInt(x: Int): UInt = x.U
   implicit def intToBoolean(x: Int): Boolean = if (x != 0) true else false
   implicit def booleanToInt(x: Boolean): Int = if (x) 1 else 0
   implicit def booleanToBool(x: Boolean): Bool = Bool(x)
   implicit def sextToConv(x: UInt) = new AnyRef {
      def sextTo(n: Int): UInt = Cat(Fill(n - x.getWidth, x(x.getWidth-1)), x)
   }

   implicit def wcToUInt(c: WideCounter): UInt = c.value
}
 
//do two masks have at least 1 bit match?
object maskMatch
{
   def apply(msk1: UInt, msk2: UInt): Bool =
   {
      val br_match = (msk1 & msk2) != 0.U
      return br_match
   }
}
   
//clear one-bit in the Mask as specified by the idx
object clearMaskBit
{
   def apply(msk: UInt, idx: UInt): UInt =
   {
      return (msk & ~(1.U << idx))(msk.getWidth-1, 0)
   }
}
  
//shift a register over by one bit
object PerformShiftRegister
{
   def apply(reg_val: Bits, new_bit: Bool): Bits =
   {
      reg_val := Cat(reg_val(reg_val.getWidth-1, 0).toBits, new_bit.toBits).toBits
      reg_val
   }
}

object Split
{
  // is there a better way to do do this?
  def apply(x: Bits, n0: Int) = {
    val w = checkWidth(x, n0)
    (x(w-1,n0), x(n0-1,0))
  }
  def apply(x: Bits, n1: Int, n0: Int) = {
    val w = checkWidth(x, n1, n0)
    (x(w-1,n1), x(n1-1,n0), x(n0-1,0))
  }
  def apply(x: Bits, n2: Int, n1: Int, n0: Int) = {
    val w = checkWidth(x, n2, n1, n0)
    (x(w-1,n2), x(n2-1,n1), x(n1-1,n0), x(n0-1,0))
  }

  private def checkWidth(x: Bits, n: Int*) = {
    val w = x.getWidth
    def decreasing(x: Seq[Int]): Boolean =
      if (x.tail.isEmpty) true
      else x.head > x.tail.head && decreasing(x.tail)
    require(decreasing(w :: n.toList))
    w
  }
}
 

// a counter that clock gates most of its MSBs using the LSB carry-out
case class WideCounter(width: Int, inc: Bool = Bool(true))
{
   private val isWide = width >= 4
   private val smallWidth = if (isWide) log2Ceil(width) else width
   private val small = Reg(init=0.asUInt(smallWidth.W))
   private val nextSmall = small + 1.asUInt((smallWidth+1).W)
   when (inc) { small := nextSmall(smallWidth-1,0) }
                      
   private val large = if (isWide) {
      val r = Reg(init=0.asUInt((width - smallWidth).W))
      when (inc && nextSmall(smallWidth)) { r := r + 1.U }
      r
   } else null
   
   val value = Cat(large, small)
   
   def := (x: UInt) = {
      val w = x.getWidth
      small := x(w.min(smallWidth)-1,0)
      if (isWide) large := (if (w < smallWidth) 0.U else x(w.min(width)-1,smallWidth))
   }
}


// taken from rocket FPU
object RegEn
{
   def apply[T <: Data](data: T, en: Bool) = 
   {
      val r = Reg(data)
      when (en) { r := data }
      r
   }
   def apply[T <: Bits](data: T, en: Bool, resetVal: T) = 
   {
      val r = RegInit(resetVal)
      when (en) { r := data }
      r
   }
}
 
object Str
{
  def apply(s: String): UInt = {
    var i = BigInt(0)
    require(s.forall(validChar _))
    for (c <- s)
      i = (i << 8) | c
    i.asUInt((s.length*8).W)
  }
  def apply(x: Char): Bits = {
    require(validChar(x))
    val lit = x.asUInt(8.W)
    lit
  }
  def apply(x: UInt): Bits = apply(x, 10)
  def apply(x: UInt, radix: Int): Bits = {
    val rad = radix.U
    val digs = digits(radix)
    val w = x.getWidth
    require(w > 0)

    var q = x
    var s = digs(q % rad)
    for (i <- 1 until ceil(log(2)/log(radix)*w).toInt) {
      q = q / rad
      s = Cat(Mux(Bool(radix == 10) && q === 0.U, Str(' '), digs(q % rad)), s)
    }
    s
  }
  def apply(x: SInt): Bits = apply(x, 10)
  def apply(x: SInt, radix: Int): Bits = {
    val neg = x < 0.S
    val abs = Mux(neg, -x, x).toUInt
    if (radix != 10) {
      Cat(Mux(neg, Str('-'), Str(' ')), Str(abs, radix))
    } else {
      val rad = UInt(radix)
      val digs = digits(radix)
      val w = abs.getWidth
      require(w > 0)

      var q = abs
      var s = digs(q % rad)
      var needSign = neg
      for (i <- 1 until ceil(log(2)/log(radix)*w).toInt) {
        q = q / rad
        val placeSpace = q === 0.U
        val space = Mux(needSign, Str('-'), Str(' '))
        needSign = needSign && !placeSpace
        s = Cat(Mux(placeSpace, space, digs(q % rad)), s)
      }
      Cat(Mux(needSign, Str('-'), Str(' ')), s)
    }
  }

  def bigIntToString(x: BigInt): String = {
    val s = new StringBuilder
    var b = x
    while (b != 0) {
      s += (x & 0xFF).toChar
      b = b >> 8
    }
    s.toString
  }

  private def digit(d: Int): Char = (if (d < 10) '0'+d else 'a'-10+d).toChar
  private def digits(radix: Int): Vec[Bits] =
    Vec((0 until radix).map(i => Str(digit(i))))

  private def validChar(x: Char) = x == (x & 0xFF)
}
 


}

