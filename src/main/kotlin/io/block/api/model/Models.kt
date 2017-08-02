package io.block.api.model

import com.google.gson.annotations.SerializedName

data class AccountAddresses(var network: String,
                            var addresses: List<Address>)

data class AccountBalance(var network: String,
                          @SerializedName("available_balance") var availableBalance: String,
                          @SerializedName("pending_received_balance") var pendingReceivedBalance: String)

data class Address(@SerializedName("user_id") var userId: Int,
                   var address: String,
                   var label: String,
                   @SerializedName("available_balance") var availableBalance: String,
                   @SerializedName("pending_received_balance") var pendingReceivedBalance: String)

data class AddressBalances(var network: String,
                           @SerializedName("available_balance") var availableBalance: String,
                           @SerializedName("pending_received_balance") var pendingReceivedBalance: String,
                           var balances: List<Balance>)

data class AddressByLabel(var network: String,
                          @SerializedName("user_id") var userId: Int,
                          var address: String,
                          var label: String,
                          @SerializedName("available_balance") var availableBalance: String,
                          @SerializedName("pending_received_balance") var pendingReceivedBalance: String)

data class Amount(var recipient: String,
                  var amount: String)

data class Balance(@SerializedName("user_id") var userId: Int,
                   var address: String,
                   var label: String,
                   @SerializedName("available_balance") var availableBalance: String,
                   @SerializedName("pending_received_balance") var pendingReceivedBalance: String)

data class EncryptedPassphrase(@SerializedName("signer_address") var signerAddress: String,
                               @SerializedName("signer_public_key") var signerPubKey: String,
                               var passphrase: String)

data class Error(@SerializedName("error_message") var message: String)

data class GreenAddress(var network: String,
                        var address: String)

data class GreenAddresses(@SerializedName("green_addresses") var greenAddresses: List<GreenAddress>)

data class GreenTransaction(var network: String,
                            var txid: String)

data class GreenTransactions(@SerializedName("green_txs") var greenTransactions: List<GreenTransaction>)

data class NewAddress(var network: String,
                      @SerializedName("user_id") var userId: Int,
                      var address: String,
                      var label: String)

data class Input(@SerializedName("input_no") var inputNo: Int,
                 @SerializedName("signatures_needed") var sigsNeeded: Int,
                 @SerializedName("data_to_sign") var dataToSign: String,
                 var signers: List<Signer>)

data class Price(var price: String,
                 @SerializedName("price_base") var priceBase: String,
                 var exchange: String,
                 var time: Long)

data class Prices(var network: String,
                  var prices: List<Price>)

data class Signer(@SerializedName("signer_address") var signerAddress: String,
                  @SerializedName("signer_public_key") var signerPubKey: String,
                  @SerializedName("signed_data") var signedData: String)

data class TransactionReceived(var txid: String,
                                @SerializedName("from_green_addresses") var fromGreenAddresses: Boolean,
                                var time: Long,
                                var confirmations: Int,
                                var amountsReceived: List<Amount>,
                                var senders: List<String>,
                                var confidence: Double,
                                @SerializedName("propagated_by_nodes") var propagatedByNodes: Int)

data class TransactionSent(var txid: String,
                           @SerializedName("from_green_addresses") var fromGreenAddresses: Boolean,
                           var time: Long,
                           var confirmations: Int,
                           @SerializedName("total_amount_sent") var totalAmountSent: String,
                           var amountsSent: List<Amount>,
                           var senders: List<String>,
                           var confidence: Double,
                           @SerializedName("propagated_by_nodes") var propagatedByNodes: Int)

data class TransactionsReceived(var network: String,
                                var txs: List<TransactionReceived>)

data class TransactionsSent(var network: String,
                            var txs: List<TransactionSent>)

data class Withdrawal(var network: String,
                      var txid: String,
                      @SerializedName("amount_withdrawn") var amountWithdrawn: String,
                      @SerializedName("amount_sent") var amountSent: String,
                      @SerializedName("network_fee") var networkFee: String,
                      @SerializedName("blockio_fee") var blockIOFee: String)

data class WithdrawSignRequest(@SerializedName("reference_id") var referenceId: String,
                               @SerializedName("more_signatures_needed") var moreSigsNeeded: Boolean,
                               var inputs: List<Input>,
                               @SerializedName("encrypted_passphrase") var encryptedPassphrase: EncryptedPassphrase)

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