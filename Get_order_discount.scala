package Labs.Project
import java.{lang, time}
import java.time.{Instant, LocalDate,ZoneId, ZonedDateTime}
import scala.io.Source
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
    val lines: List[String] = Source.fromFile("src/main/scala/labs/Project/TRX1000.csv").getLines().toList.drop(1)
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
    lines
      .map(mapLinesToOrders)
      .map(x => applyRules(x,rules_tuple))
      .map(_.sortWith(_ > _).take(2))
      .map { discounts => if (discounts.nonEmpty) discounts.sum / discounts.length else 0.0}
      .foreach(println)




}
