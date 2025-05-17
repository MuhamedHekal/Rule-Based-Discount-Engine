package Labs.CalculateDiscountScala

import java.util.logging.{Logger, FileHandler, Level, LogRecord}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LoggerFactory {
  val logger: Logger = Logger.getLogger("RulesEngineLogger")
  val fileHandler = new FileHandler("src/main/scala/labs/CalculateDiscountScala/rules_engine.log", true)

  class CustomFormatter extends java.util.logging.Formatter {
    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override def format(record: LogRecord): String = {
      val timestamp = dtf.format(LocalDateTime.now())
      val level = record.getLevel.getName
      val message = record.getMessage
      s"$timestamp $level $message\n"
    }
  }

  fileHandler.setFormatter(new CustomFormatter())
  logger.addHandler(fileHandler)
  logger.setUseParentHandlers(false)
  logger.setLevel(Level.ALL)
}