package freechips.rocketchip.tile

import chisel3._
import chisel3.util.experimental.BoringUtils
import org.chipsalliance.cde.config.Field

/** Observation only. Intended for one Rocket hart, one Gemmini, CUSTOM_3. */
case object RoCCRunLedDebugKey extends Field[Boolean](false)

object RoCCRunLedDebug {
  val opcode = 0x7b
  val config1 = 16
  val run = 15
  val cpuPresented = "rocc_run_led_cpu_presented"
  val cpuAccepted = "rocc_run_led_cpu_accepted"
  val gemminiAccepted = "rocc_run_led_gemmini_accepted"
  val loopAccepted = "rocc_run_led_loop_accepted"
  val firstCommand = "rocc_run_led_first_command"
  val channels = Seq(cpuPresented, cpuAccepted, gemminiAccepted, loopAccepted, firstCommand)

  /** Each observer clears at its own CONFIG_1 handshake. The ordered stream
    * keeps epochs aligned for the single12 test, which fences each tile.
    * Event wins if clear and event coincide. Never drives functional control.
    */
  def latch(clear: Bool, event: Bool, name: String): Bool = {
    val seen = RegInit(false.B).suggestName(name)
    when (clear || event) { seen := event }
    // Named wiring is supported by Chisel 6.7. Keep only these small observation
    // registers; do not preserve entire controllers or add functional ports.
    BoringUtils.addSource(seen, name)
    seen
  }

  def sink(name: String): Bool = {
    val value = WireDefault(false.B).suggestName(name + "_sink")
    BoringUtils.addSink(value, name)
    value
  }
}
