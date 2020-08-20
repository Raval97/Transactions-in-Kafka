package AlpakkaKafka.Model

class Receipt {

  val countOfProducts = 1000000

  def produkty(): List[Product] = {
    val product = new Product(0, "łosos", 5, 10)
    var products = List.fill(countOfProducts)(product)
    products
  }

//  val countOfProducts = 30
//
//  var products = List[Product]()
//  products :+= (new Product(1, "łosos", 5, 10))
//  products :+= (new Product(2, "banan", 2, 3))
//  products :+= (new Product(3, "woda", 3, 2))
//  products :+= (new Product(4, "chleb", 1, 4.60))
//  products :+= (new Product(5, "jogrt", 1, 3.20))
//  products :+= (new Product(6, "ryz", 3, 15))
//  products :+= (new Product(7, "baton", 1, 15))
//  products :+= (new Product(8, "cukier", 1, 2.5))
//  products :+= (new Product(9, "makaron", 4, 1.3))
//  products :+= (new Product(10, "ser", 0.5, 25))
//  products :+= (new Product(11, "łosos", 5, 10))
//  products :+= (new Product(12, "banan", 2, 3))
//  products :+= (new Product(13, "woda", 3, 2))
//  products :+= (new Product(14, "chleb", 1, 4.60))
//  products :+= (new Product(15, "jogrt", 1, 3.20))
//  products :+= (new Product(16, "ryz", 3, 15))
//  products :+= (new Product(17, "baton", 1, 15))
//  products :+= (new Product(18, "cukier", 1, 2.5))
//  products :+= (new Product(19, "makaron", 4, 1.3))
//  products :+= (new Product(20, "ser", 0.5, 25))
//  products :+= (new Product(21, "łosos", 5, 10))
//  products :+= (new Product(22, "banan", 2, 3))
//  products :+= (new Product(23, "woda", 3, 2))
//  products :+= (new Product(24, "chleb", 1, 4.60))
//  products :+= (new Product(25, "jogrt", 1, 3.20))
//  products :+= (new Product(26, "ryz", 3, 15))
//  products :+= (new Product(27, "baton", 1, 15))
//  products :+= (new Product(28, "cukier", 1, 2.5))
//  products :+= (new Product(29, "makaron", 4, 1.3))
//  products :+= (new Product(30, "ser", 0.5, 25))

}
