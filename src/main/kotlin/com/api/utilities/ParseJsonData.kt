package com.api.utilities

import org.apache.log4j.LogManager
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths

class ParseJsonData(private val filePath: String) {
    private var indexDerivativeData: JSONObject? = parseJSONFile(filePath)
    private val logger = LogManager.getLogger(ParseJsonData::class.java)
    var vWap= BigDecimal.valueOf(0.00)

    fun getAtTheMoneyPrice(): Int? {
        return indexDerivativeData?.getInt("underlyingValue")
    }
    fun getMarketDirectionDerivativeData(
        optionExpiry: String,
        atheMoneyValue: Int,
        callDataMap: HashMap<Int, MarketDirectionData>,
        putDataMap: HashMap<Int, MarketDirectionData>,
    ) {
       //
        //data["instrumentType"]
        var isVWAPFound= false
        val indexDerivativeData: JSONObject? = parseJSONFile(filePath)
        val jsonArray = indexDerivativeData?.getJSONArray("stocks") as JSONArray
        for (value in jsonArray) {
            for (value in jsonArray) {
                val metadata: JSONObject = value as JSONObject
                val marketDeptOrderBook = metadata.getJSONObject("marketDeptOrderBook")
                val tradeInfo = marketDeptOrderBook.getJSONObject("tradeInfo")
                val data = metadata.getJSONObject("metadata")
//                data["expiryDate"].equals("30-Sep-2021")  &&
                if (!isVWAPFound &&  (data["instrumentType"].equals("Index Futures" )||data["instrumentType"].equals("Stock Futures" ))){
                    val vWapValue = tradeInfo.get("vmap")
                    if (vWapValue is Int) {
                        vWap = BigDecimal.valueOf(vWapValue.toInt() + 0.00)
                    }
                    if (vWapValue is BigDecimal) {
                        vWap = vWapValue
                    }
                    isVWAPFound=true
                }
                if ( data["expiryDate"].equals(optionExpiry) && data["optionType"].equals("Call")
                ) {
                    var changeInOpenInterest = 0
                    val changeOI = tradeInfo["changeinOpenInterest"]
                    val keyStrikePrice:Int = data["strikePrice"] as Int
                    if (keyStrikePrice != atheMoneyValue){
                        if (changeOI is Int){
                            changeInOpenInterest =  tradeInfo["changeinOpenInterest"] as Int

                        }
                        if (changeOI is BigDecimal){
                            changeInOpenInterest = changeOI.toBigInteger().intValueExact()

                        }
                    }

                    callDataMap[keyStrikePrice] = MarketDirectionData(changeInOpenInterest)
                }
                if (data["expiryDate"].equals(optionExpiry) && data["optionType"].equals("Put")
                ) {
                    val keyStrikePrice:Int = data["strikePrice"] as Int
                    var changeInOpenInterest = 0
                    val changeOI = tradeInfo["changeinOpenInterest"]

                    if (keyStrikePrice != atheMoneyValue) {
                        if (changeOI is Int){
                            changeInOpenInterest =  tradeInfo["changeinOpenInterest"] as Int

                        }
                        if (changeOI is BigDecimal){
                            changeInOpenInterest = changeOI.toBigInteger().intValueExact()

                        }
                    }
                    putDataMap[keyStrikePrice] = MarketDirectionData(changeInOpenInterest)
                }


            }

        }
        logger.debug("call market data : $callDataMap")
        logger.debug("put market data : $putDataMap")
    }
     fun getDerivativeData(
        filePath: String,
        optionExpiry: String,
        strikePriceArray: IntArray,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>,
    ) {
        val indexDerivativeData: JSONObject? = parseJSONFile(filePath)
        val jsonArray = indexDerivativeData?.getJSONArray("stocks") as JSONArray
        for (strikePrice in strikePriceArray) {
            println(strikePrice)
            var isCallDataFound = false
            var isPutDataFound = false
            for (value in jsonArray) {
                val metadata: JSONObject = value as JSONObject
                val marketDeptOrderBook = metadata.getJSONObject("marketDeptOrderBook")
                val tradeInfo = marketDeptOrderBook.getJSONObject("tradeInfo")
                val data = metadata.getJSONObject("metadata")
                if (data["strikePrice"] == strikePrice && data["expiryDate"].equals(optionExpiry) && data["optionType"].equals(
                        "Call"
                    )
                ) {
                    val callVolume = data["numberOfContractsTraded"] as Int
                    println("Call Volume for $strikePrice strikePrice : $callVolume")
                    var changeInOpenInterest :Int= 0
                    val changeOI = tradeInfo["changeinOpenInterest"]
                    if (changeOI is Int){
                        changeInOpenInterest =  changeOI

                    }
                    if (changeOI is BigDecimal){
                        changeInOpenInterest = changeOI.toBigInteger().intValueExact()

                    }
                    val callVWAP = tradeInfo["vmap"]
                    val callLTP = data["lastPrice"]
                    var callVWAPDouble: BigDecimal = BigDecimal.valueOf(0.00)
                    var callLTPDouble: BigDecimal = BigDecimal.valueOf(0.00)
                    if (callVWAP is Int) {
                        callVWAPDouble = BigDecimal.valueOf(callVWAP.toInt() + 0.00)
                    }
                    if (callVWAP is BigDecimal) {
                        callVWAPDouble = callVWAP
                    }
                    if (callLTP is Int) {
                        callLTPDouble = BigDecimal.valueOf(callLTP.toInt() + 0.00)
                    }
                    if (callLTP is BigDecimal) {
                        callLTPDouble = callLTP
                    }
                    callDataMap[strikePrice] =
                        DerivativeData(callVolume, changeInOpenInterest, callVWAPDouble, callLTPDouble)
                    isCallDataFound = true
                }
                if (data["strikePrice"] == strikePrice && data["expiryDate"].equals(optionExpiry) && data["optionType"].equals(
                        "Put"
                    )
                ) {
                    val putVolume = data["numberOfContractsTraded"] as Int
                    println("put Volume for $strikePrice strikePrice : $putVolume")
                    var changeInOpenInterest :Int= 0
                    val changeInOI = tradeInfo["changeinOpenInterest"]
//                    val changeInOpenInterest = tradeInfo["changeinOpenInterest"] as Int
                    if (changeInOI is Int){
                        changeInOpenInterest =  changeInOI

                    }
                    if (changeInOI is BigDecimal){
                        changeInOpenInterest = changeInOI.toBigInteger().intValueExact()

                    }


                    val putVWAP = tradeInfo["vmap"]
                    var putVWAPDouble: BigDecimal = BigDecimal.valueOf(0.00)
                    if (putVWAP is Int) {
                        putVWAPDouble = BigDecimal.valueOf(putVWAP.toInt() + 0.00)
                    }
                    if (putVWAP is BigDecimal) {
                        putVWAPDouble = putVWAP
                    }
                    val putLTP = data["lastPrice"]
                    var putLTPDouble: BigDecimal = BigDecimal.valueOf(0.00)
                    if (putLTP is Int) {
                        putLTPDouble = BigDecimal.valueOf(putLTP.toInt() + 0.00)
                    }
                    if (putLTP is BigDecimal) {
                        putLTPDouble = putLTP
                    }
                    putDataMap[strikePrice] =
                        DerivativeData(putVolume, changeInOpenInterest, putVWAPDouble, putLTPDouble)
                    isPutDataFound = true
                }
                if (isCallDataFound && isPutDataFound) {
                    logger.debug("$strikePrice : call and put Data Found")


                    break
                }
            }
        }
         logger.debug("CallData : $callDataMap")
         logger.debug("PutData  : $putDataMap")
    }

    private fun parseJSONFile(filename: String): JSONObject? {
        return try {
            val content = String(Files.readAllBytes(Paths.get(filename)))
            JSONObject(content)
        } catch (e: Exception) {
            logger.debug("parseJSONFile :  Exception found while reading JSON file $filename")
            null
        }


    }
}