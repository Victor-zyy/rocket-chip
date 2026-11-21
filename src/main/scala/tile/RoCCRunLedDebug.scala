package freechips.rocketchip.tile

import chisel3._
import chisel3.util.experimental.BoringUtils
import org.chipsalliance.cde.config.{Field, Parameters}

/** Observation only. Intended for one Rocket hart, one Gemmini, CUSTOM_3. */
case object RoCCRunLedDebugKey extends Field[Boolean](false)

/** Elaboration-time references, scoped to this diagnostic configuration.
  * This is not hardware storage and adds no control path back into the DUT.
  */
final class RoCCRunLedDebugSignals {
  private val sources = scala.collection.mutable.Map.empty[String, Bool]
  def record(name: String, value: Bool): Unit = {
    require(!sources.contains(name), s"Duplicate RUN LED source: $name")
    sources(name) = value
  }
  def source(name: String): Bool = sources.getOrElse(name,
    throw new IllegalArgumentException(s"Missing RUN LED source: $name; instantiate the DUT first"))
}
case object RoCCRunLedDebugSignalsKey extends Field[Option[RoCCRunLedDebugSignals]](None)

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
  def latch(clear: Bool, event: Bool, name: String)(implicit p: Parameters): Bool = {
    val seen = RegInit(false.B).suggestName(name)
    when (clear || event) { seen := event }
    p(RoCCRunLedDebugSignalsKey).getOrElse(
      throw new IllegalArgumentException("RUN LED configuration has no signal registry"))
      .record(name, seen)
    seen
  }

  def sink(name: String)(implicit p: Parameters): Bool = {
    val source = p(RoCCRunLedDebugSignalsKey).getOrElse(
      throw new IllegalArgumentException("RUN LED configuration has no signal registry")).source(name)
    // Use Chisel 6's direct, single-argument bore. Ports/connections are created
    // during elaboration, including through ChipTop. No legacy FIRRTL wiring
    // annotations are emitted (the FPGA flow reads embedded + sidecar annos).
    BoringUtils.bore(source).suggestName(name + "_sink")
  }
}
