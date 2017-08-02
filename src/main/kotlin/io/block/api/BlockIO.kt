package io.block.api

import com.google.gson.Gson
import io.block.api.Constants.Methods
import io.block.api.Constants.Params
import io.block.api.Constants.Values
import io.block.api.Response.*
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.net.URISyntaxException
import java.text.NumberFormat
import java.util.*

class BlockIO(private val apiKey: String) {

    enum class ParamType {
        ADDRESSES,
        LABELS,
        USER_IDS
    }

    /**
     * Requests the balance of the account associated with this clients' API key
     * @return An {@link io.block.api.AccountBalance} object containing the balances
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getAccountBalance(): AccountBalance {
        return doApiCall<ResponseAccountBalance>(Methods.GET_ACCOUNT_BALANCE, null).accountBalance
    }

    /**
     * Requests the creation of a new address for the account associated with the clients' API key
     * @param label Optional label for the new address. null or "" for random label
     * @return A {@link io.block.api.NewAddress} object containing information about the new address
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getNewAddress(label: String? = null): NewAddress {
        var params: Map<String, String>? = null
        if (label != null) {
            params = mapOf(Pair(Params.LABEL, label))
        }
        return doApiCall<ResponseNewAddress>(Methods.GET_NEW_ADDRESS, params).newAddress
    }

    /**
     * Requests a list of addresses in the account associated with this clients' API key
     * @return An {@link io.block.api.AccountAddresses} object containing the addresses
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getAccountAddresses(): AccountAddresses {
        return doApiCall<ResponseAccountAddresses>(Methods.GET_MY_ADDRESSES, null).accountAddresses
    }

    /**
     * Requests balance(s) of given address(es) in the account associated with this clients' API key <br>
     * Make sure that the addresses actually exist in the account or the whole call will fail
     * @param addresses A String array containing the addresses to request balances for
     * @return An {@link io.block.api.AddressBalances} object containing the balances
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getAddressBalancesByAddress(addresses: Array<String>): AddressBalances {
        if (addresses.isEmpty()) {
            throw IllegalArgumentException("You have to provide at least one address.")
        }

        val paramString = if (addresses.size == 1) addresses[0] else Arrays.asList(*addresses).toString().replace(Regex("^\\[|]$"), "")
        val params = mapOf(Pair(Params.ADDRESSES, paramString))

        return doApiCall<ResponseAddressBalances>(Methods.GET_ADDRESS_BALANCE, params).accountBalances
    }

    /**
     * Requests balance(s) of given label(s) in the account associated with this clients' API key <br>
     * Make sure that the labels actually exist in the account or the whole call will fail
     * @param labels A String array containing the labels to request balances for
     * @return An {@link io.block.api.AddressBalances} object containing the balances
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getAddressBalancesByLabels(labels: Array<String>): AddressBalances {
        if (labels.isEmpty()) {
            throw IllegalArgumentException("You have to provide at least one label.")
        }

        val paramString = if (labels.size == 1) labels[0] else Arrays.asList(*labels).toString().replace(Regex("^\\[|]$"), "")
        val params = mapOf(Pair(Params.LABELS, paramString))

        return doApiCall<ResponseAddressBalances>(Methods.GET_ADDRESS_BALANCE, params).accountBalances
    }

    /**
     * Requests the address with the given label from the account associated with this clients' API key
     * @param label The label for which to request the address for
     * @return An {@link io.block.api.AddressByLabel} object containing the address and additional info about it
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getAddressByLabel(label: String): AddressByLabel {
        if (label.isEmpty()) {
            throw IllegalArgumentException("You have to provide a valid label.")
        }

        val params = mapOf(Pair(Params.LABEL, label))
        return doApiCall<ResponseAddressByLabel>(Methods.GET_ADDRESS_BY_LABEL, params).addressByLabel
    }

    /**
     * Convenience method for a simple withdrawal from the account associated with this clients' API key to the specified address
     * @param address Target address
     * @param amount Amount to withdraw
     * @param secretPin The secret PIN you set at block.io to authorize and sign the withdrawal
     * @return A {@link io.block.api.Withdrawal} object containing information about the sent transaction.
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun withdrawToAddress(address: String, amount: Double, secretPin: String): Withdrawal {
        val target = mapOf(Pair(address, amount))
        return withdraw(null, null, target, ParamType.ADDRESSES, secretPin)
    }

    /**
     * Withdraw from the account associated with this clients' API key.
     * @param sources Supply an array of sources for this withdrawal. If you set this to null, then block.io will select the sources automatically.
     *                You must not mix source types!
     * @param sourceType If you supplied a sources array, this is mandatory. Must be one of {@link io.block.api.BlockIO.ParamType}.
     * @param targetsAndAmounts A {@link java.util.Map} with target as key ({@link java.lang.String}) and amount as value ({@link java.lang.Double}).
     *                          Each entry will be one target of the withdrawal. Limit is 100 per withdrawal.
     *                          You must not mix target types!
     * @param targetType This is mandatory and defines what type of targets this withdrawal goes to. One of {@link io.block.api.BlockIO.ParamType}.
     * @param secretPin The secret PIN you set at block.io to authorize and sign the withdrawal
     * @return A {@link io.block.api.Withdrawal} object containing information about the sent transaction.
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun withdraw(sources: Array<String>?, sourceType: ParamType?, targetsAndAmounts: Map<String, Double>, targetType: ParamType,
                 secretPin: String): Withdrawal {
        if (targetsAndAmounts.isEmpty()) {
            throw IllegalArgumentException("You have to provide between one and 100 pair(s) of targets and amounts to withdraw to.")
        }

        if (secretPin.isEmpty()) {
            throw IllegalArgumentException("You have to provide your secret pin for withdrawals")
        }

        val params = setupWithdrawalParams(targetsAndAmounts, targetType)
        var method = Methods.WITHDRAW_FROM_ANY
        if (sources != null && sources.isNotEmpty()) {
            val sourcesString = if (sources.size == 1) sources[0] else Arrays.asList(*sources).toString().replace(Regex("^\\[|]$"), "")
            when (sourceType) {
                ParamType.ADDRESSES -> {
                    params[Params.FROM_ADDRESSES] = sourcesString
                    method = Methods.WITHDRAW_FROM_ADDRESSES
                }

                ParamType.LABELS -> {
                    params[Params.FROM_LABELS] = sourcesString
                    method = Methods.WITHDRAW_FROM_LABELS
                }

                ParamType.USER_IDS -> {
                    params[Params.FROM_USER_IDS] = sourcesString
                    method = Methods.WITHDRAW_FROM_USER_IDS
                }

                else -> {
                    throw BlockIOException("You requested a withdrawal from specific sources but did not set the source type.")
                }
            }
        }

        val signRequest = doPostApiCall<ResponseWithdrawSignRequest>(method, params).withdrawSignRequest
        return finalizeWithdrawal(signRequest, secretPin)
    }

    @Throws(BlockIOException::class)
    private fun setupWithdrawalParams(addressesAndAmounts: Map<String, Double>, targetType: ParamType): HashMap<String, String> {
        var addressesParamString = ""
        var amountsParamString = ""

        val format = NumberFormat.getNumberInstance(Locale.US) // This will force '.' as decimal separator
        for ((address, amount) in addressesAndAmounts) {
            addressesParamString += address + ","
            amountsParamString += format.format(amount) + ","
        }

        addressesParamString = addressesParamString.replace(Regex(",$"), "")
        amountsParamString = amountsParamString.replace(Regex(",$"), "")

        val params = HashMap<String, String>(2)
        when (targetType) {
            ParamType.ADDRESSES -> {
                params[Params.TO_ADDRESSES] = addressesParamString
            }

            ParamType.LABELS -> {
                params[Params.TO_LABELS] = addressesParamString
            }

            ParamType.USER_IDS -> {
                params[Params.TO_USER_IDS] = addressesParamString
            }
        }
        params[Params.AMOUNTS] = amountsParamString
        return params
    }

    @Throws(BlockIOException::class)
    private fun finalizeWithdrawal(signRequest: WithdrawSignRequest, secretPin: String): Withdrawal {
        SigningUtils.signWithdrawalRequest(signRequest, secretPin)

        val gson = Gson()
        val params = mapOf(Pair(Params.SIG_DATA, gson.toJson(signRequest, WithdrawSignRequest::class.java)))
        return doPostApiCall<ResponseWithdrawal>(Methods.WITHDRAW_DO_FINAL, params).withdrawal
    }

    /**
     * Requests prices of the currency of the account associated with this clients' API key
     * @param baseCurrency Optional base currency to return prices in. null or "" to get prices in all available base currencies
     * @return A {@link io.block.api.Prices} object containing price information in one or more base currency from one or more exchange
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getPrices(baseCurrency: String?): Prices {
        var params: Map<String, String>? = null
        if (baseCurrency != null && baseCurrency != "") {
            params = mapOf(Pair(Params.PRICE_BASE, baseCurrency))
        }

        val response = doApiCall<ResponsePrices>(Methods.GET_PRICES, params)
        return response.prices
    }

    /**
     * Checks the given address(es) for being Block.io Green Address(es)
     * @param addresses A String array containing the addresses to request status for
     * @return An {@link io.block.api.GreenAddresses} object containing the subset of the given addresses that are green
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun isGreenAddress(addresses: Array<String>): GreenAddresses {
        if (addresses.isEmpty()) {
            throw IllegalArgumentException("You have to provide at least one address.")
        }

        val paramString = if (addresses.size == 1) addresses[0] else Arrays.asList(*addresses).toString().replace("^\\[|]$".toRegex(), "")
        val params = mapOf(Pair(Params.ADDRESSES, paramString))

        return doApiCall<ResponseGreenAddresses>(Methods.IS_GREEN_ADDRESS, params).greenAddresses
    }

    /**
     * Checks the given transaction(s) for being sent by a Block.io Green Address
     * @return An [io.block.api.model.GreenTransactions] object containing the subset of the given transactions that are green
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun isGreenTransaction(txIDs: Array<String>): GreenTransactions {
        if (txIDs.isEmpty()) {
            throw IllegalArgumentException("You have to provide at least one transaction ID.")
        }

        val paramString = if (txIDs.size == 1) txIDs[0] else Arrays.asList(*txIDs).toString().replace("^\\[|]$".toRegex(), "")
        val params = mapOf(Pair(Params.TX_IDS, paramString))

        val response = doApiCall<ResponseGreenTransactions>(Methods.IS_GREEN_TX, params)
        return response.greenTransactions
    }

    /**
     * Lists up to 100 of the last transactions received by the account associated with this clients' API key
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsReceived] object containing the list of received transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsReceived(beforeTX: String): TransactionsReceived {
        return abstractTransactionRequest(null, null, beforeTX, Values.TYPE_RECEIVED) as TransactionsReceived
    }

    /**
     * Lists up to 100 of the last transactions received by the provided addresses of the account associated with this clients' API key
     * @param addresses A String array containing the addresses to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsReceived] object containing the list of received transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsReceivedByAddress(addresses: Array<String>, beforeTX: String): TransactionsReceived {
        return abstractTransactionRequest(addresses, Params.ADDRESSES, beforeTX, Values.TYPE_RECEIVED) as TransactionsReceived
    }

    /**
     * Lists up to 100 of the last transactions received by the provided labels of the account associated with this clients' API key
     * @param labels A String array containing the labels to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsReceived] object containing the list of received transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsReceivedByLabel(labels: Array<String>, beforeTX: String): TransactionsReceived {
        return abstractTransactionRequest(labels, Params.LABELS, beforeTX, Values.TYPE_RECEIVED) as TransactionsReceived
    }

    /**
     * Lists up to 100 of the last transactions received by the provided user IDs of the account associated with this clients' API key
     * @param userIDs A String array containing the user IDs to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsReceived] object containing the list of received transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsReceivedByUserID(userIDs: Array<String>, beforeTX: String): TransactionsReceived {
        return abstractTransactionRequest(userIDs, Params.USER_IDS, beforeTX, Values.TYPE_RECEIVED) as TransactionsReceived
    }

    /**
     * Lists up to 100 of the last transactions sent by the account associated with this clients' API key
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsSent] object containing the list of sent transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsSent(beforeTX: String): TransactionsSent {
        return abstractTransactionRequest(null, null, beforeTX, Values.TYPE_SENT) as TransactionsSent
    }

    /**
     * Lists up to 100 of the last transactions sent by the provided addresses of the account associated with this clients' API key
     * @param addresses A String array containing the addresses to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsSent] object containing the list of sent transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsSentByAddress(addresses: Array<String>, beforeTX: String): TransactionsSent {
        return abstractTransactionRequest(addresses, Params.ADDRESSES, beforeTX, Values.TYPE_SENT) as TransactionsSent
    }

    /**
     * Lists up to 100 of the last transactions sent by the provided labels of the account associated with this clients' API key
     * @param labels A String array containing the labels to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsSent] object containing the list of sent transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsSentByLabel(labels: Array<String>, beforeTX: String): TransactionsSent {
        return abstractTransactionRequest(labels, Params.LABELS, beforeTX, Values.TYPE_SENT) as TransactionsSent
    }

    /**
     * Lists up to 100 of the last transactions sent by the provided user IDs of the account associated with this clients' API key
     * @param userIDs A String array containing the user IDs to request transactions for
     * *
     * @param beforeTX An optional transaction ID used as upper bound of the requested transactions. Use this to request more than 100 of the last transactions
     * *
     * @return A [io.block.api.model.TransactionsSent] object containing the list of sent transactions
     * *
     * @throws BlockIOException
     */
    @Throws(BlockIOException::class)
    fun getTransactionsSentByUserID(userIDs: Array<String>, beforeTX: String): TransactionsSent {
        return abstractTransactionRequest(userIDs, Params.USER_IDS, beforeTX, Values.TYPE_SENT) as TransactionsSent
    }

    @Throws(BlockIOException::class)
    private fun abstractTransactionRequest(whatFor: Array<String>?, typeOfParams: String?, beforeTx: String?, type: String): Any {
        val params = hashMapOf(Pair(Params.TYPE, type))

        if (whatFor != null && typeOfParams != null) {
            if (whatFor.isEmpty()) {
                throw IllegalArgumentException("You have to provide at least one address/label/user ID.")
            }
            params[typeOfParams] = if (whatFor.size == 1) whatFor[0] else Arrays.asList(*whatFor).toString().replace(Regex("^\\[|]$"), "")
        }

        if (beforeTx != null && beforeTx != "") {
            params[Params.BEFORE_TX] = beforeTx
        }

        if (type == Values.TYPE_RECEIVED) {
            val response = doApiCall<ResponseTransactionsReceived>(Methods.GET_TXNS, params)
            return response.transactionsReceived
        } else if (type == Values.TYPE_SENT) {
            val response = doApiCall<ResponseTransactionsSent>(Methods.GET_TXNS, params)
            return response.transactionsSent
        } else {
            throw IllegalArgumentException("Internal error. Please file an issue report")
        }
    }

    inline @Throws(BlockIOException::class)
    private fun <reified T : Response> doApiCall(method: String, params: Map<String, String>?): T {
        val client = HttpClients.createDefault()
        val request = HttpGet(Constants.buildUri(method))
        val uriBuilder = URIBuilder(request.uri)
        uriBuilder.addParameter(Params.API_KEY, apiKey)

        if (params != null) {
            for ((key, value) in params) {
                uriBuilder.addParameter(key, value)
            }
        }

        val response: CloseableHttpResponse
        try {
            request.uri = uriBuilder.build()
            response = client.execute(request)
            return getResponse(response)
        } catch (e: IOException) {
            throw BlockIOException("Network connectivity problem.")
        } catch (e: URISyntaxException) {
            throw BlockIOException("URI build failed. That is an internal error. Please file an issue.")
        }

    }

    inline @Throws(BlockIOException::class)
    private fun <reified T : Response> doPostApiCall(method: String, params: Map<String, String>?): T {
        val client = HttpClients.createDefault()
        val request = HttpPost(Constants.buildUri(method, true))
        val postParams = ArrayList<NameValuePair>(2)

        postParams.add(BasicNameValuePair(Params.API_KEY, apiKey))

        if (params != null) {
            for ((key, value) in params) {
                postParams.add(BasicNameValuePair(key, value))
            }
        }

        val response: CloseableHttpResponse
        try {
            request.entity = UrlEncodedFormEntity(postParams)
            response = client.execute(request)
            return getResponse(response)
        } catch (e: IOException) {
            throw BlockIOException("Network connectivity problem.")
        }

    }

    inline @Throws(BlockIOException::class)
    private fun <reified T : Response> getResponse(response: CloseableHttpResponse): T {
        val gson = Gson()
        val responseString: String
        try {
            responseString = EntityUtils.toString(response.entity)
            response.close()
        } catch (e: IOException) {
            throw BlockIOException("Received invalid data from API.")
        }

        when (response.statusLine.statusCode) {
            HttpStatus.SC_OK -> return gson.fromJson(responseString, T::class.java)
            HttpStatus.SC_NOT_FOUND -> {
                val error = gson.fromJson(responseString, ResponseError::class.java) as ResponseError
                throw BlockIOException("API returned error: " + error.error.message)
            }
            else -> throw BlockIOException("Unknown API response.")
        }
    }

}

class BlockIOException(message: String) : Exception(message)