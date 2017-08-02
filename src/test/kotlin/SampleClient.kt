import io.block.api.BlockIO
import io.block.api.BlockIOException
import java.util.*

fun main(args: Array<String>) {
    val api = BlockIO("API KEY")
    try {
        println()

        val (network, availableBalance, pendingReceivedBalance) = api.getAccountBalance()
        println("Balance for account " + network
                + ": Confirmed: " + availableBalance
                + " Pending: " + pendingReceivedBalance)
        println()

        val prices = api.getPrices("")
        for (price in prices.prices) {
            println("Price for " + prices.network
                    + " in " + price.priceBase
                    + " on " + price.exchange
                    + " is " + price.price
                    + " as of " + Date(price.time * 1000).toString())
        }
        println()

        val (_, txid, amountWithdrawn, amountSent, networkFee, fee) = api.withdrawToAddress("ADDRESS", 50.0, "SECRETPIN")
        println("Withdrawal done. Transaction ID: " + txid
                + " Amount withdrawn: " + amountWithdrawn
                + " Amount sent: " + amountSent
                + " Network fee: " + networkFee
                + " Block.io fee: " + fee)
        println()
    } catch (e: BlockIOException) {
        e.printStackTrace()
    }
}