package AlpakkaKafka.Model

class Product(var id: Int, val name: String, val amount: Double, val price: Double){

  override def toString = s"$id, $name, $amount, $price"
}




