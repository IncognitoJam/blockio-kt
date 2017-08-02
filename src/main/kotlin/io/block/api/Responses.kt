package io.block.api

import com.google.gson.annotations.SerializedName

open class Response(var status: String) {

    class ResponseAccountBalance(status: String, @SerializedName("data") var accountBalance: AccountBalance): Response(status)
    class ResponseNewAddress(status: String, @SerializedName("data") var newAddress: NewAddress): Response(status)
    class ResponseAccountAddresses(status: String, @SerializedName("data") var accountAddresses: AccountAddresses): Response(status)
    class ResponseAddressBalances(status: String, @SerializedName("data") var accountBalances: AddressBalances): Response(status)
    class ResponseAddressByLabel(status: String, @SerializedName("data") var addressByLabel: AddressByLabel): Response(status)
    class ResponseWithdrawal(status: String, @SerializedName("data") var withdrawal: Withdrawal): Response(status)
    class ResponseWithdrawSignRequest(status: String, @SerializedName("data") var withdrawSignRequest: WithdrawSignRequest): Response(status)
    class ResponsePrices(status: String, @SerializedName("data") var prices: Prices): Response(status)
    class ResponseGreenAddresses(status: String, @SerializedName("data") var greenAddresses: GreenAddresses): Response(status)
    class ResponseGreenTransactions(status: String, @SerializedName("data") var greenTransactions: GreenTransactions): Response(status)
    class ResponseTransactionsReceived(status: String, @SerializedName("data") var transactionsReceived: TransactionsReceived): Response(status)
    class ResponseTransactionsSent(status: String, @SerializedName("data") var transactionsSent: TransactionsSent): Response(status)
    class ResponseError(status: String, @SerializedName("data") var error: Error): Response(status)

}