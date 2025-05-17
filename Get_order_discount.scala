package Labs.CalculateDiscountScala
import LoggerFactory.logger
import scala.util.Using
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}
import scala.io.Source
import java.sql.{Connection, DriverManager, PreparedStatement}
import java.time.temporal.ChronoUnit
object Get_order_discount extends App {
    // cass class to store orders
    case class Order(
          timestamp: Instant,
          product_name : String,
          expiry_date : LocalDate,
          quantity : Int,
          unit_price : Double,
          channel : String,
          payment_method : String)
    // read the orders in list of string each index has one order
    val lines: List[String] = Source.fromFile("src/main/scala/labs/CalculateDiscountScala/TRX1000.csv").getLines().toList.drop(1)
    //print(orders)
    def mapLinesToOrders(line : String): Order = {
      val data = line.split(',')
      Order(Instant.parse(data(0)),data(1),LocalDate.parse(data(2)),data(3).toInt,data(4).toDouble,data(5),data(6))
    }
    // check for expiry date
    def expireDate_rule(order: Order) : Boolean = {
      val now = LocalDate.now()
      val remainingDays = ChronoUnit.DAYS.between(now, order.expiry_date)
      remainingDays < 30 & remainingDays > 0
    }
    def expireDate_discount(order: Order) : Double = {
      val now = LocalDate.now()
      val remaining_days = ChronoUnit.DAYS.between(now, order.expiry_date)
      //println(s" now is $now order expiry date ${order.expiry_date} and the remainging days is ${remaining_days} ")
      if (remaining_days >= 30) 0.toDouble else (30 - remaining_days).toDouble
    }

    // check for product name
    def cheeseOrWine_rule(order: Order): Boolean = {
      val name = order.product_name.split(' ')(0)
      if ((name == "Wine") | (name == "Cheese")) true else false
    }
    def cheeseOrWine_Discount(order: Order) : Double = {
      val name = order.product_name.split(' ')(0)
      if (name == "Wine") 5.toDouble else if (name == "Cheese") 10.toDouble else 0.toDouble
    }

    // check for 23 march rule
    def _23rdOfMarch_rule(order: Order) : Boolean = {
      val order_date = order.timestamp
      val zonedDateTime = ZonedDateTime.ofInstant(order_date,ZoneId.of("UTC"))
      if ((zonedDateTime.getDayOfMonth == 23) & (zonedDateTime.getMonthValue == 3)) true else false
    }
    def _23rdOfMarch_discount(order: Order) :Double = {
      val order_date = order.timestamp
      val zonedDateTime = ZonedDateTime.ofInstant(order_date,ZoneId.of("UTC"))
      if ((zonedDateTime.getDayOfMonth == 23) & (zonedDateTime.getMonthValue == 3)) 50.toDouble else 0.toDouble
    }

    //check for quantity rule
    def moreThan5_rule(order: Order) : Boolean= {
      val quantity = order.quantity
      if (quantity >5) true else false
    }
    def moreThan5_discount(order: Order) : Double= {
      val quantity = order.quantity
      if ((quantity >= 6) & (quantity <= 9)) 5.toDouble
      else if ((quantity >= 10) & (quantity <= 14)) 7.toDouble
      else if (quantity >= 15) 10.toDouble
      else 0.toDouble
    }

    // check fot channel
    def from_app_rule(order: Order) :Boolean = {
      if (order.channel == "App") true else false
    }
    def from_app_dicscount(order: Order) :Double = {
      (((order.quantity + 4) / 5) * 5).toDouble
    }

    // check payment method
    def visaCard_rule(order: Order) : Boolean = {
      if (order.payment_method == "Visa") true else false
    }
    def visaCard_discount(order: Order) : Double = {
      5.toDouble
    }

    val rules_tuple = List(
      (expireDate_rule _, expireDate_discount _),
      (cheeseOrWine_rule _, cheeseOrWine_Discount _),
      (_23rdOfMarch_rule _, _23rdOfMarch_discount _),
      (moreThan5_rule _, moreThan5_discount _),
      (from_app_rule _, from_app_dicscount _),
      (visaCard_rule _, visaCard_discount _)
    )

    def applyRules(order: Order, tuple: List[(Order => Boolean, Order=> Double)]): List[Double] = {
      tuple.collect {case (check, calcDis) if check(order) => calcDis(order)}
  }


    val dbUrl = "jdbc:postgresql://127.0.0.1:5432/postgres"
    val dbUser = "postgres"
    val dbPassword = "123"
    // Function to store complete order with average discount
    def storeCompleteOrder(order: Order, avgDiscount: Double): Unit = {
      val sql = """
    INSERT INTO order_discounts (
      order_timestamp, product_name, expiry_date, quantity,
      unit_price, channel, payment_method, average_discount, TotalPrice
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
  """

      Using(DriverManager.getConnection(dbUrl, dbUser, dbPassword)) { conn =>
        Using(conn.prepareStatement(sql)) { pstmt =>
          pstmt.setTimestamp(1, java.sql.Timestamp.from(order.timestamp))
          pstmt.setString(2, order.product_name)
          pstmt.setDate(3, java.sql.Date.valueOf(order.expiry_date))
          pstmt.setInt(4, order.quantity)
          pstmt.setDouble(5, order.unit_price)
          pstmt.setString(6, order.channel)
          pstmt.setString(7, order.payment_method)
          pstmt.setDouble(8, avgDiscount)
          val totalPrice = (order.quantity * order.unit_price) * (1 - avgDiscount / 100)
          pstmt.setDouble(9, totalPrice)

          pstmt.executeUpdate()
        }
      }.recover {
        case e => println(s"Error storing order: ${e.getMessage}")
        logger.severe(s"Error storing order: ${e.getMessage}")
      }
    }


  logger.info(s"Read ${lines.length} orders from CSV file.")
  lines
    .map(mapLinesToOrders)
    .map { order =>
      logger.info(s"Processing order for product: ${order.product_name}")

      val discounts = applyRules(order, rules_tuple)
        .sortWith(_ > _)
        .take(2)
      logger.info(s"Discounts applied: ${discounts.mkString(", ")}")

      val avgDiscount = if (discounts.nonEmpty) discounts.sum / discounts.length else 0.0
      logger.info(f"Average discount calculated: $avgDiscount%.2f%%")

      // Store complete order data with average discount
      try {
        storeCompleteOrder(order, avgDiscount)
        logger.info(s"Stored order for ${order.product_name} with $avgDiscount% average discount.")
      } catch {
        case e: Exception =>
          logger.severe(s"Failed to store order for ${order.product_name}: ${e.getMessage}")
      }

      // Print confirmation
      println(s"Stored order for ${order.product_name} with ${avgDiscount}% average discount")
      avgDiscount
    }

}
