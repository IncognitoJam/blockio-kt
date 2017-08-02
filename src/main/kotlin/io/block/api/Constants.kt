package io.block.api

object Constants {

    const val BASE_URL = "https://block.io/api/v"
    const val API_VERSION = "2"

    object Methods {
        const val GET_NEW_ADDRESS = "get_new_address"
        const val GET_ACCOUNT_BALANCE = "get_balance"
        const val GET_MY_ADDRESSES = "get_my_addresses"
        const val GET_ADDRESS_BALANCE = "get_address_balance"
        const val GET_ADDRESS_BY_LABEL = "get_address_by_label"

        const val WITHDRAW_FROM_ANY = "withdraw"
        const val WITHDRAW_FROM_LABELS = "withdraw_from_labels"
        const val WITHDRAW_FROM_ADDRESSES = "withdraw_from_addresses"
        const val WITHDRAW_FROM_USER_IDS = "withdraw_from_user_ids"
        const val WITHDRAW_DO_FINAL = "sign_and_finalize_withdrawal"

        const val GET_PRICES = "get_current_price"
        const val IS_GREEN_ADDRESS = "is_green_address"
        const val IS_GREEN_TX = "is_green_transaction"
        const val GET_TXNS = "get_transactions"
    }

    object Params {
        const val API_KEY = "api_key"
        const val LABEL = "label"
        const val LABELS = "labels"
        const val ADDRESSES = "addresses"
        const val FROM_LABELS = "from_labels"
        const val TO_LABELS = "to_labels"
        const val FROM_ADDRESSES = "from_addresses"
        const val TO_ADDRESSES = "to_addresses"
        const val FROM_USER_IDS = "from_user_ids"
        const val TO_USER_IDS = "to_user_ids"
        const val AMOUNTS = "amounts"
        const val PIN = "pin"
        const val SIG_DATA = "signature_data"

        const val PRICE_BASE = "price_base"
        const val TX_IDS = "transaction_ids"
        const val TYPE = "type"
        const val BEFORE_TX = "before_tx"
        const val USER_IDS = "user_ids"
    }

    object Values {
        const val TYPE_SENT = "sent"
        const val TYPE_RECEIVED = "received"
    }

    fun buildUri(method: String, trailingSlash: Boolean = true) = BASE_URL + API_VERSION + "/" + method + if (trailingSlash) "/" else ""

}