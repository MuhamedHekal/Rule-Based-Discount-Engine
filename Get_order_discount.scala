package Labs.Project
import java.{lang, time}
import java.time.{Instant, LocalDate,ZoneId, ZonedDateTime}
import scala.io.Source
import java.sql.{Connection, DriverManager, PreparedStatement}
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
    val lines: List[String] = Source.fromFile("src/main/scala/labs/Calculate-Discount-Scala/TRX1000.csv").getLines().toList.drop(1)
    //print(orders)
    def mapLinesToOrders(line : String): Order = {
      val data = line.split(',')
      Order(Instant.parse(data(0)),data(1),LocalDate.parse(data(2)),data(3).toInt,data(4).toDouble,data(5),data(6))
    }
    // check for expiry date
    def expireDate_rule(order: Order) : Boolean = {
      val now = LocalDate.now()
      if (now.until(order.expiry_date).getDays() < 30 )true else false
    }
    def expireDate_discount(order: Order) : Double = {
      val now = LocalDate.now()
      val remaining_days = now.until(order.expiry_date).getDays()
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

    val rules_tuple = List(
      (expireDate_rule _, expireDate_discount _),
      (cheeseOrWine_rule _,cheeseOrWine_Discount _),
      (_23rdOfMarch_rule _,_23rdOfMarch_discount _),
      (moreThan5_rule _,moreThan5_discount _)
    )

    def applyRules(order: Order, tuple: List[(Order => Boolean, Order=> Double)]): List[Double] = {
      tuple.collect {case (check, calcDis) if check(order) => calcDis(order)}
  }


    val dbUrl = "jdbc:postgresql://127.0.0.1:5432/postgres"
    val dbUser = "postgres"
    val dbPassword = "123"
    // Function to store complete order with average discount
    def storeCompleteOrder(order: Order, avgDiscount: Double): Unit = {
      var conn: Connection = null
      var pstmt: PreparedStatement = null

      try {
        conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)

        val sql = """
        INSERT INTO order_discounts (
          order_timestamp, product_name, expiry_date, quantity,
          unit_price, channel, payment_method, average_discount , TotalPrice
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)
      """

        pstmt = conn.prepareStatement(sql)

        // Set all order parameters
        pstmt.setTimestamp(1, java.sql.Timestamp.from(order.timestamp))
        pstmt.setString(2, order.product_name)
        pstmt.setDate(3, java.sql.Date.valueOf(order.expiry_date))
        pstmt.setInt(4, order.quantity)
        pstmt.setDouble(5, order.unit_price)
        pstmt.setString(6, order.channel)
        pstmt.setString(7, order.payment_method)
        pstmt.setDouble(8, avgDiscount)
        pstmt.setDouble(9, (order.quantity * order.unit_price) - (order.quantity * order.unit_price * (avgDiscount /100)))

        pstmt.executeUpdate()
      } catch {
        case e: Exception =>
          println(s"Failed to store order: ${e.getMessage}")
      } finally {
        if (pstmt != null) pstmt.close()
        if (conn != null) conn.close()
      }
    }



  lines
    .map(mapLinesToOrders)
    .map { order =>
      val discounts = applyRules(order, rules_tuple)
        .sortWith(_ > _)
        .take(2)
      val avgDiscount = if (discounts.nonEmpty) discounts.sum / discounts.length else 0.0

      // Store complete order data with average discount
      storeCompleteOrder(order, avgDiscount)

      // Print confirmation
      println(s"Stored order for ${order.product_name} with ${avgDiscount}% average discount")
      avgDiscount
    }

}
