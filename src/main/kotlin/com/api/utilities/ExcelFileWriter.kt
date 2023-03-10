package com.api.utilities

import com.api.constants.ProjectProperties
import org.apache.log4j.Logger
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.testng.Assert
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.math.BigDecimal


class ExcelFileWriter(
    private val optionExpirtDate: String,
    private val symbol: String,
    private val strikePrice: String
) {
    private val OUTPUT_DIRECTORY: String = ProjectProperties.ROOT_JSON_FOLDER_PATH
    private var WORKBOOK_NAME = optionExpirtDate.replace("-", "")
    private lateinit var currentWorksheet: String
    private val fileName =
        "${Util().getFolderRootPath()}$OUTPUT_DIRECTORY/${TimeUtil.getDate()}_${WORKBOOK_NAME}_${symbol}_${strikePrice}.xlsx"
    private var workbook: XSSFWorkbook = XSSFWorkbook()
    private lateinit var defaultWorkSheet: Sheet
    private val logger = Logger.getLogger("ExcelFileWriter")
    private val referenceFilePath = "${Util().getFolderRootPath()}/src/main/resources/ReferenceSheet.xlsx"


    //Write Headers to file by default
    private fun addSheetHeaders() {
        val row: org.apache.poi.ss.usermodel.Row = defaultWorkSheet.createRow(0)
        row.createCell(0).setCellValue("TIME")
        row.createCell(1).setCellValue("CALL_OI_CHANGE")
        row.createCell(2).setCellValue("PUT_OI_CHANGE")
        row.createCell(3).setCellValue("OI_DIFFERENCE")
        row.createCell(4).setCellValue("PCR")
        row.createCell(5).setCellValue("SIGNAL")
        row.createCell(6).setCellValue("CALL_VWAP")
        row.createCell(7).setCellValue("PUT_VWAP")
        row.createCell(8).setCellValue("CALL_LTP")
        row.createCell(9).setCellValue("PUT_LTP")
        row.createCell(10).setCellValue("CALL_VOLUME")
        row.createCell(11).setCellValue("PUT_VOLUME")
        row.createCell(12).setCellValue("VOLUME_DIFFERENCE")
        autoSizeColumns(defaultWorkSheet, 4)
        logger.debug("Added Headers with styles to the Sheet")
    }


    private fun addNewWorkSheets() {
        if (workbook.getSheet(currentWorksheet) == null || workbook.getSheet(currentWorksheet).equals("")) {
            defaultWorkSheet = workbook.createSheet(currentWorksheet);
            logger.debug("Added Sheet: $currentWorksheet")
        } else {
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Retrieving Sheet: $currentWorksheet")
        }
    }

    private fun addNewWorkBook(referenceFilePath: String) {
        if (workbook.getSheet(currentWorksheet) == null || workbook.getSheet(currentWorksheet).equals("")) {
            val inputStream = FileInputStream(referenceFilePath)
            workbook = XSSFWorkbook(inputStream)
            defaultWorkSheet = workbook.createSheet(currentWorksheet);
            logger.debug("Added Sheet: $currentWorksheet")
        } else {
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Retrieving Sheet: $currentWorksheet")
        }
    }

    private fun autoSizeColumns(sheet: Sheet, lastColumnNumber: Int) {
        for (num in 0..lastColumnNumber) {
            sheet.autoSizeColumn(num)
        }
    }


    private fun makeDirectory(dirPath: String?): Boolean? {
        return if (dirPath != null) {
            try {
                val dir = File(dirPath)
                dir.mkdirs()
                logger.debug("Creating output_dir for storing End Report Analysis file :$dirPath")
                true
            } catch (e: Exception) {
                logger.debug(" Exception while creating [" + dirPath + "] output_dir path  : " + e.message)
                false
            }
        } else {
            logger.debug("Path to create output_dir is sent as Null")
            null
        }
    }

    fun commit(strikePrice: Int) {
        try {
            val fos = FileOutputStream(fileName)
            workbook.write(fos)
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        logger.debug("For $strikePrice : Saving changes to $fileName")
    }

    fun writeDataToFile(
        expiryDate: String,
        strikePrice: Int,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>
    ) {
        currentWorksheet = strikePrice.toString()

        if (File(fileName).exists()) {
            if (checkWorkSheetExists()) {
                println("worksheet found for $strikePrice")
                addDataToExistingSheet(strikePrice, callDataMap, putDataMap)
            } else {
                println("worksheet not found for $strikePrice, adding sheet")
                addNewWorkSheets()
                addSheetHeaders()
                addDataToNewSheet(strikePrice, callDataMap, putDataMap)
            }
        } else {
            val referenceFilePath = "${Util().getFolderRootPath()}/src/main/resources/ReferenceSheet.xlsx"
            makeDirectory(OUTPUT_DIRECTORY)
            if (File(referenceFilePath).exists()) {
                println("Reference sheet found, creating new entry for $strikePrice sheet")
                //create excel and add data
                addNewWorkBook(referenceFilePath)
                addSheetHeaders()
                addDataToNewSheet(strikePrice, callDataMap, putDataMap)
            } else {
                println("Reference sheet not found, creating new file sheet")
                addNewWorkSheets()
                addSheetHeaders()
                addDataToNewSheet(strikePrice, callDataMap, putDataMap)
            }


        }
        commit(strikePrice)


    }

    private fun addDataToNewSheet(
        strikePrice: Int,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>
    ) {
        val lastColumnNumber = defaultWorkSheet.lastRowNum
        val row = defaultWorkSheet.createRow(lastColumnNumber + 1)
        writeData(callDataMap, strikePrice, putDataMap, row as XSSFRow)
    }

    private fun addDataToExistingSheet(
        strikePrice: Int,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>
    ) {
        val inputStream = FileInputStream(fileName)
        workbook = XSSFWorkbook(inputStream)
        val currentWorksheet = workbook.getSheet(currentWorksheet)
        val lastColumnNumber = currentWorksheet.lastRowNum
        val row = currentWorksheet.createRow(lastColumnNumber + 1)
        writeData(callDataMap, strikePrice, putDataMap, row)
    }

    private fun writeData(
        callDataMap: HashMap<Int, DerivativeData>,
        strikePrice: Int,
        putDataMap: HashMap<Int, DerivativeData>,
        row: XSSFRow
    ) {
        val callData = callDataMap[strikePrice]
        val putData = putDataMap[strikePrice]
        logger.debug("adding data for $strikePrice")
        logger.debug("$strikePrice : call changeInOPenInterest : ${callData?.changeInOPenInterest}")
        logger.debug("$strikePrice : put changeInOPenInterest : ${putData?.changeInOPenInterest}")
        logger.debug("$strikePrice : callData.vwap : ${callData?.vwap}")
        logger.debug("$strikePrice : putData.vwap  : ${putData?.vwap}")
        logger.debug("$strikePrice : callData.ltp : ${callData?.ltp}")
        logger.debug("$strikePrice : putData.ltp  : ${putData?.ltp}")
        logger.debug("$strikePrice : callData.volume : ${callData?.volume}")
        logger.debug("$strikePrice : putData.volume  : ${putData?.volume}")
        row.createCell(0).setCellValue(TimeUtil.getExecutionTimeStamp().toDouble())
        row.createCell(1).setCellValue((0.00 + callData?.changeInOPenInterest!!))
        row.createCell(2).setCellValue(0.00 + putData?.changeInOPenInterest!!)
        val totalOpenInterestChange = row.createCell(3)
        val totalChangeInOpenInterest = putData.changeInOPenInterest!! - callData.changeInOPenInterest!! + 0.00
        totalOpenInterestChange.setCellValue(totalChangeInOpenInterest)
        val tradeSignalCell = row.createCell(4)
        if (setStyle(totalOpenInterestChange, totalChangeInOpenInterest)) {
            tradeSignalCell.setCellValue("BUY")
            tradeSignal(tradeSignalCell, true)
        } else {
            tradeSignalCell.setCellValue("SELL")
            tradeSignal(tradeSignalCell, false)
        }
        row.createCell(5).setCellValue(callData.vwap!!.toDouble())
        row.createCell(6).setCellValue(putData.vwap!!.toDouble())
        row.createCell(7).setCellValue(callData.ltp!!.toDouble())
        row.createCell(8).setCellValue(putData.ltp!!.toDouble())
        row.createCell(9).setCellValue(callData.volume!! + 0.00)
        row.createCell(10).setCellValue(0.00 + putData.volume!!)
        row.createCell(11).setCellValue(0.00 + putData.volume!! - callData.volume!!)

    }

    private fun checkWorkSheetExists(): Boolean {
        val inputStream = FileInputStream(fileName)
        workbook = XSSFWorkbook(inputStream)
        return workbook.getSheet(currentWorksheet) != null
    }

    fun addDataToDefaultSheet(
        strikePriceStringArray: List<String>,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>
    ) {
        var isOnlyOneStrikePriceEntry: Boolean = false
        var strikePrice: Int = 0

        currentWorksheet = if (strikePriceStringArray.size == 1) {
            isOnlyOneStrikePriceEntry = true
            strikePrice = strikePriceStringArray[0].toInt()
            "INTRADAY_DATA_${strikePriceStringArray[0]}"


        } else {
            "INTRADAY_DATA"
        }

        if (workbook.getSheetName(0).equals("SAMPLE_SUMMARY")) {
            //workbook.getSheet(currentWorksheet) == null || workbook.getSheet(currentWorksheet).equals("") ||
            val inputStream = FileInputStream(fileName)
            workbook = XSSFWorkbook(inputStream)
            workbook.setSheetName(0, currentWorksheet)
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Selected Sheet: $defaultWorkSheet")
            val lastColumnNumber = defaultWorkSheet.lastRowNum
            val row: Row = defaultWorkSheet.createRow(lastColumnNumber)
            writeSummaryData(row, callDataMap, putDataMap, isOnlyOneStrikePriceEntry, strikePrice)
        } else if (workbook.getSheet(currentWorksheet) == null) {
            defaultWorkSheet = workbook.createSheet(currentWorksheet);
            logger.debug("Added Sheet: $currentWorksheet")
            val row: org.apache.poi.ss.usermodel.Row = defaultWorkSheet.createRow(0)
            row.createCell(0).setCellValue("TIME")
            row.createCell(1).setCellValue("Total Call OI Change")
            row.createCell(2).setCellValue("Total Put OI Change")
            row.createCell(3).setCellValue("OI_DIFFERENCE")
            row.createCell(4).setCellValue("PCR")
            row.createCell(5).setCellValue("SIGNAL")
            row.createCell(6).setCellValue("CALL_VWAP")
            row.createCell(7).setCellValue("PUT_VWAP")
            row.createCell(8).setCellValue("CALL_LTP")
            row.createCell(9).setCellValue("PUT_LTP")
            row.createCell(10).setCellValue("CALL_VOLUME")
            row.createCell(11).setCellValue("PUT_VOLUME")
            row.createCell(12).setCellValue("VOLUME_DIFFERENCE")
            if (isOnlyOneStrikePriceEntry) {
                row.createCell(8).setCellValue("CALL_VWAP")
                row.createCell(9).setCellValue("CALL_LTP")
                row.createCell(10).setCellValue("PUT_VWAP")
                row.createCell(11).setCellValue("PUT_LTP")
                row.createCell(12).setCellValue("Avg Call Ltp")
                row.createCell(13).setCellValue("Avg Put Ltp")
                row.createCell(14).setCellValue("Avg Ltp difference")

            }
            autoSizeColumns(defaultWorkSheet, 4)
            logger.debug("Added Headers with styles to the Sheet")
            val lastColumnNumber = defaultWorkSheet.lastRowNum
            val newRow: Row = defaultWorkSheet.createRow(lastColumnNumber + 1)
            writeSummaryData(newRow, callDataMap, putDataMap, isOnlyOneStrikePriceEntry, strikePrice)
        } else {
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Retrieving Sheet: $currentWorksheet")
            val inputStream = FileInputStream(fileName)
            workbook = XSSFWorkbook(inputStream)
            val currentWorksheet = workbook.getSheet(currentWorksheet)
            val lastColumnNumber = currentWorksheet.lastRowNum
            val row = currentWorksheet.createRow(lastColumnNumber + 1)
            writeSummaryData(row, callDataMap, putDataMap, isOnlyOneStrikePriceEntry, strikePrice)


        }
        commit(strikePrice)

    }

    private fun writeSummaryData(
        row: Row,
        callDataMap: HashMap<Int, DerivativeData>,
        putDataMap: HashMap<Int, DerivativeData>,
        isOnlyOneStrikePriceEntry: Boolean,
        strikePrice: Int
    ) {
        val timeStampCell = row.createCell(0)
        timeStampCell.setCellValue(TimeUtil.getExecutionTimeStamp().toDouble())

        var totalCallChangeInOpenInterest = 0.00
        var totalPutChangeInOpenInterest = 0.00
        var totalCallVolume = 0.0
        var totalPutVolume = 0.0
        var totalCallLtp = 0.00
        var totalPutLtp = 0.00
        var pcr = 0.00

        for ((key, value) in callDataMap) {
            print("key value in callDataMap $key")
            totalCallChangeInOpenInterest += callDataMap[key]?.changeInOPenInterest!!
            totalCallVolume += callDataMap[key]?.volume!!
            totalCallLtp += callDataMap[key]?.ltp!!.toDouble()
            print("OI change value in callDataMap after adding ${callDataMap[key]?.changeInOPenInterest!!}  = $totalCallChangeInOpenInterest")
            print("Volume change in callDataMap after adding ${callDataMap[key]?.volume!!}  = $totalCallVolume")

        }
        for ((key, value) in putDataMap) {
            print("key value in putDataMap $key")
            totalPutChangeInOpenInterest += putDataMap[key]?.changeInOPenInterest!!
            totalPutVolume += putDataMap[key]?.volume!!
            totalPutLtp += putDataMap[key]?.ltp!!.toDouble()
            print("value in putDataMap after adding ${putDataMap[key]?.changeInOPenInterest!!}  = $totalPutChangeInOpenInterest")
            print("Volume change in callDataMap after adding ${putDataMap[key]?.volume!!}  = $totalPutVolume")
        }

        val averageCallLtp = totalCallLtp / callDataMap.size
        val averagePutLtp = totalPutLtp / putDataMap.size

        val totalCallChangeInOpenIny: Cell = row.createCell(1)

        totalCallChangeInOpenIny.setCellValue(totalCallChangeInOpenInterest)

        val totalPutChangeInOpenInterestCell = row.createCell(2)
        totalPutChangeInOpenInterestCell.setCellValue(totalPutChangeInOpenInterest)

        val totalOpenInterestChange = row.createCell(3)
        val pcrCell = row.createCell(4)
        var pcrValue = totalPutChangeInOpenInterest / totalCallChangeInOpenInterest
        if (pcrValue < 0) {
            pcrValue *= -1
        }
        setStyle(pcrCell, pcrValue)
        pcrCell.setCellValue(pcrValue)
        val tradeSignalCell = row.createCell(5)
        val totalChangeInOpenInterest = totalPutChangeInOpenInterest - totalCallChangeInOpenInterest
        totalOpenInterestChange.setCellValue(totalChangeInOpenInterest)
        setStyle(totalOpenInterestChange, totalChangeInOpenInterest)
        if (setStyle(totalOpenInterestChange, totalChangeInOpenInterest)) {
            tradeSignalCell.setCellValue("BUY")
            tradeSignal(tradeSignalCell, true)
        } else {
            tradeSignalCell.setCellValue("SELL")
            tradeSignal(tradeSignalCell, false)

        }
        row.createCell(6).setCellValue(totalCallVolume)
        row.createCell(7).setCellValue(totalPutVolume)

        val totalVolumeChange = row.createCell(8)
        val totalVolumeChangeInVolume = totalPutVolume - totalCallVolume
        totalVolumeChange.setCellValue(totalVolumeChangeInVolume)
        setStyle(totalVolumeChange, totalVolumeChangeInVolume)


        if (isOnlyOneStrikePriceEntry) {
            val callData = callDataMap[strikePrice]
            val putData = putDataMap[strikePrice]
            row.createCell(9).setCellValue(callData?.vwap!!.toDouble())
            row.createCell(10).setCellValue(callData.ltp!!.toDouble())
            row.createCell(11).setCellValue(putData?.vwap!!.toDouble())
            row.createCell(12).setCellValue(putData.ltp!!.toDouble())
            row.createCell(13).setCellValue(averageCallLtp)
            row.createCell(14).setCellValue(averagePutLtp)
            row.createCell(15).setCellValue(averagePutLtp - averageCallLtp)
        }

            if (row.rowNum > 2) {
                var previousRow: org.apache.poi.ss.usermodel.Row = defaultWorkSheet.getRow( row.rowNum- 1)
//                var row = defaultWorkSheet.getRow(row.rowNum)
                val currentRowCallTotalOI = row.getCell(1)
                val currentRowPutTotalOI = row.getCell(2)
                val currentTotalOIDiff = row.getCell(3)
                val prevRowCallTotalOI = previousRow.getCell(1)
                val prevRowPutTotalOI = previousRow.getCell(2)
                val prevRowTotalOiDiff = previousRow.getCell(3)

                logger.debug("current row data at index ${row.rowNum} is " + row.getCell(0))
                logger.debug("current Total Call OI Change at index  ${row.rowNum} is   " + currentRowCallTotalOI)
                logger.debug("current Total Put OI Change at index  ${row.rowNum} is  " + currentRowPutTotalOI)
                logger.debug("current Total OI_DIFFERENCE at index  ${row.rowNum} is  " + currentTotalOIDiff)
                logger.debug("previous row data at index ${row.rowNum-1} is " + previousRow.getCell(0))
                logger.debug("previous Total Call OI Change at index  ${row.rowNum-1} is   " + prevRowCallTotalOI)
                logger.debug("previous Total Put OI Change at index  ${row.rowNum-1} is  " + prevRowPutTotalOI)
                logger.debug("previous Total OI_DIFFERENCE at index  ${row.rowNum-1} is  " + prevRowTotalOiDiff)

                val updatedSignalCell: Cell = row.createCell(17)
                val updatedAlgoSignalCell: Cell = row.createCell(18)
                when {
                    currentTotalOIDiff.numericCellValue > 50000 -> {
                        if (currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue > 10000) {
                            logger.debug(" oi difference between current and previous entry : ${currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue}")
                            updatedSignalCell.setCellValue("BUY")
                            tradeSignal(updatedSignalCell, true)
                        }
                    }

                    currentTotalOIDiff.numericCellValue < -50000 -> {
                        if (currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue < -10000) {
                            logger.debug(" oi difference between current and previous entry : ${currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue}")
                            updatedSignalCell.setCellValue("SELL")
                            tradeSignal(updatedSignalCell, false)
                        }
                    }
                }

                if (((currentRowPutTotalOI.numericCellValue - prevRowPutTotalOI.numericCellValue) < -4000) && (((currentRowCallTotalOI.numericCellValue - prevRowCallTotalOI.numericCellValue) >2000)) &&( (currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue) < -8000)) {
                    //&& ((totalChangeInOpenInterest-previousOiDifference) >5000)
                    updatedAlgoSignalCell.setCellValue("BUY PUT")
                    tradeSignal(updatedAlgoSignalCell, false)
                }

                if (((currentRowCallTotalOI.numericCellValue - prevRowCallTotalOI.numericCellValue) < -4000) && (((currentRowPutTotalOI.numericCellValue - prevRowPutTotalOI.numericCellValue) > 2000))&&( (currentTotalOIDiff.numericCellValue - prevRowTotalOiDiff.numericCellValue) > 8000)) {
                    updatedAlgoSignalCell.setCellValue("BUY CALL")
                    tradeSignal(updatedAlgoSignalCell, true)
                }
            }



    }

    private fun tradeSignal(cell: Cell, boolean: Boolean) {
        if (boolean) {
            val style = workbook.createCellStyle()
            val font = workbook.createFont()
            font.color = IndexedColors.GREEN.getIndex()
            style.setFont(font)

            cell.cellStyle = style
        } else {
            val style = workbook.createCellStyle()
            val font = workbook.createFont()
            font.color = IndexedColors.RED.getIndex()
            style.setFont(font)
            cell.cellStyle = style

        }


    }

    private fun setStyle(cell: Cell, value: Double): Boolean {
        return if (value >= 1) {
            setPassStyle(cell)
            true
        } else {
            setFailStyle(cell)
            false
        }
    }

    private fun setFailStyle(cell: Cell) {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.color = IndexedColors.RED.getIndex()
        style.setFont(font)
        cell.cellStyle = style


    }

    private fun setPassStyle(cell: Cell) {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.color = IndexedColors.GREEN.getIndex()
        style.setFont(font)
        cell.cellStyle = style

    }

    fun addMarketDirectionRawData(
        marketDirectionStrikePriceArray: IntArray,
        underlyingValue: Int,
        callDataMap: HashMap<Int, MarketDirectionData>,
        putDataMap: HashMap<Int, MarketDirectionData>,
        marketDirectionVWAP: BigDecimal
    ) {
        currentWorksheet = "RAW_DATA"
        if (File(fileName).exists()) {
            if (checkWorkSheetExists()) {
                println("worksheet found for $currentWorksheet")
                defaultWorkSheet = workbook.getSheet(currentWorksheet)
                addDataToExistingRawSheet(underlyingValue, callDataMap, putDataMap, marketDirectionVWAP)
            } else {
                addNewWorkBook(fileName)
                val atTheMoneyStrikePrice =
                    marketDirectionStrikePriceArray[(marketDirectionStrikePriceArray.size - 1) / 2]
                val strikePointDifference = marketDirectionStrikePriceArray[1] - marketDirectionStrikePriceArray[0]
                addStrikePriceEntries(strikePointDifference, atTheMoneyStrikePrice)
                createNewRawSheetHeaders(underlyingValue, marketDirectionVWAP)
                addDataToNewRawSheet(underlyingValue, callDataMap, putDataMap)

            }
        } else {
            Assert.fail()
        }
        commit(strikePrice.toInt())

    }

    private fun addStrikePriceEntries(strikePricePointDifference: Int, underlyingValue: Int) {
        val strikePriceVariation = 60
        var firstStrikePrice = underlyingValue - (strikePricePointDifference * strikePriceVariation)
        defaultWorkSheet.createRow(0).createCell(0).setCellValue("StrikePrice")
        for (i in 1..(strikePriceVariation * 2) + 1) {
            defaultWorkSheet.createRow(i).createCell(0).setCellValue(firstStrikePrice.toDouble())
            firstStrikePrice += strikePricePointDifference
        }
        commit(underlyingValue)

    }

    private fun addDataToExistingRawSheet(
        underlyingValue: Int,
        callDataMap: HashMap<Int, MarketDirectionData>,
        putDataMap: HashMap<Int, MarketDirectionData>,
        marketDirectionVWAP: BigDecimal
    ) {
        defaultWorkSheet.getRow(0)
        val firstRow = defaultWorkSheet.getRow(0)
        val lastCellNumber = firstRow.lastCellNum
        val executionCount = ((lastCellNumber + 1) / 4) + 1
//
        firstRow.createCell(lastCellNumber + 1)
            .setCellValue("${executionCount}.TIME_${TimeUtil.getExecutionTimeStamp()}-${underlyingValue}#$marketDirectionVWAP")
        firstRow.createCell(lastCellNumber + 2).setCellValue("Call OI Change")
        firstRow.createCell(lastCellNumber + 3).setCellValue("Put OI Change")
        var rowNumber = 1

        val firstStrikePrice = callDataMap.toSortedMap().firstKey()
        for(entry in callDataMap.toSortedMap()){
            for (i in 1..defaultWorkSheet.lastRowNum) {
                if (defaultWorkSheet.getRow(i).getCell(0).toString().substringBefore(".") == entry.key.toString()) {
                    rowNumber = i;
                        val currentRow = defaultWorkSheet.getRow(rowNumber)
                        currentRow.createCell(lastCellNumber + 1).setCellValue(0.0 + entry.key.toInt())
                        currentRow.createCell(lastCellNumber + 2).setCellValue(0.0 + entry.value.changeInOPenInterest!!)
                        currentRow.createCell(lastCellNumber + 3).setCellValue(0.0 + putDataMap[entry.key]?.changeInOPenInterest!!)
                    }


            }
        }


    }

    private fun createNewRawSheetHeaders(underlyingValue: Int, marketDirectionVWAP: BigDecimal) {
        val firstRow = defaultWorkSheet.createRow(0)
        firstRow.createCell(1)
            .setCellValue("1.TIME_${TimeUtil.getExecutionTimeStamp()}-${underlyingValue}#$marketDirectionVWAP")
        firstRow.createCell(2).setCellValue("Call OI Change")
        firstRow.createCell(3).setCellValue("Put OI Change")


    }

    private fun addDataToNewRawSheet(
        underlyingValue: Int,
        callDataMap: HashMap<Int, MarketDirectionData>,
        putDataMap: HashMap<Int, MarketDirectionData>
    ) {
        var rowNumber = 1
        val firstStrikePrice = callDataMap.toSortedMap().firstKey()

        for(entry in callDataMap.toSortedMap()){
            for (i in 1..defaultWorkSheet.lastRowNum) {
                if (defaultWorkSheet.getRow(i).getCell(0).toString().substringBefore(".") == entry.key.toString()) {
                    rowNumber = i;
                    val currentRow = defaultWorkSheet.getRow(rowNumber)
                    currentRow.createCell( 1).setCellValue(0.0 + entry.key.toInt())
                    currentRow.createCell(  2).setCellValue(0.0 + entry.value.changeInOPenInterest!!)
                    currentRow.createCell(  3).setCellValue(0.0 + putDataMap[entry.key]?.changeInOPenInterest!!)
                }

            }
        }
    }

    fun writeMarketDirectionSummaryData(strikePriceArray: IntArray) {
        // read data from the RAW_DATA Sheet
        defaultWorkSheet = workbook.getSheet("RAW_DATA")

        val firstRow = defaultWorkSheet.getRow(0).lastCellNum
        var dataEntryList: IntArray
        var count = 0
        for (cellNumber in 0..firstRow) {
            var cellValue = defaultWorkSheet.getRow(0).getCell(cellNumber)?.stringCellValue
            if (cellValue != null && cellValue.contains("TIME")) {
                count += 1
            }
        }
        dataEntryList = IntArray(count)
        count = 0
        for (cellNumber in 0..firstRow) {
            var cellValue = defaultWorkSheet.getRow(0).getCell(cellNumber)?.stringCellValue
            if (cellValue != null && cellValue.contains("TIME")) {
                cellValue = cellValue.substringBefore(".")
                dataEntryList[count] = cellValue.toInt()
                count += 1
            }
        }
        logger.debug(dataEntryList.asList())
        val oiDataMap = HashMap<Int, ChangeOIData>(dataEntryList.size)
        for (dataEntry in dataEntryList) {
            var totalCallVolume = 0
            var totalPutVolume = 0
            var timeStamp = 0
            var underlyingValue = 0
            var marketDirectionVWAP = 0.00
            val firstRow = defaultWorkSheet.getRow(0).lastCellNum
            for (cellNumber in 0..firstRow) {
                var cellValue = defaultWorkSheet.getRow(0).getCell(cellNumber)?.stringCellValue
                var dataFound = false
                if (cellValue != null && cellValue.contains("$dataEntry.TIME_")) {

                    val lastRow: Int = defaultWorkSheet.lastRowNum
                    for (value in strikePriceArray) {

                        for (i in 1..lastRow) {
                            val currentRow = defaultWorkSheet.getRow(i)
                            if (currentRow.getCell(cellNumber) != null && currentRow.getCell(cellNumber).toString()
                                    .substringBefore(".").contains(value.toString())
                            ) {
                                val callVolume = currentRow.getCell(cellNumber + 1).toString().substringBefore(".")
                                val putVolume = currentRow.getCell(cellNumber + 2).toString().substringBefore(".")
                                totalCallVolume += callVolume.toInt()
                                totalPutVolume += putVolume.toInt()
                                timeStamp =
                                    defaultWorkSheet.getRow(0).getCell(cellNumber).toString().substringAfter("_")
                                        .substringBefore("-").toInt()
                                underlyingValue =
                                    defaultWorkSheet.getRow(0).getCell(cellNumber).toString().substringAfter("-")
                                        .substringBefore("#").toInt()
                                marketDirectionVWAP =
                                    0.00 + defaultWorkSheet.getRow(0).getCell(cellNumber).toString().substringAfter("#")
                                        .toDouble()
                                oiDataMap[dataEntry] =
                                    ChangeOIData(
                                        underlyingValue,
                                        timeStamp,
                                        totalCallVolume,
                                        totalPutVolume,
                                        marketDirectionVWAP
                                    )
                                dataFound = true
                                break
                            }
                        }
                    }

                }

                if (dataFound) {
                    break
                }
            }
            logger.debug("ENTRY $dataEntry; TIME : $timeStamp; Strike price : $strikePrice; Total Call Volume : $totalCallVolume; Total Put Volume : $totalPutVolume")
        }
        writeOIData(oiDataMap)
        commit(strikePrice.toInt())
    }

    private fun writeOIData(oiDataMap: HashMap<Int, ChangeOIData>) {
        currentWorksheet = "MARKET_DIRECTION"
        if (workbook.getSheetName(1).equals("MARKET_DIRECTION_SAMPLE")) {
            val inputStream = FileInputStream(fileName)
            workbook = XSSFWorkbook(inputStream)
            workbook.setSheetName(1, currentWorksheet)
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Selected Sheet: $defaultWorkSheet")
            var currentRow = 1
            for (oiData in oiDataMap) {
                val time = oiData.value.timeStamp
                val underliningValue = oiData.value.currentUnderlyingValue

                val totalCallOiChange = oiData.value.callChangeOI
                val totalPutOiChange = oiData.value.putchangeOI

                val row = defaultWorkSheet.createRow(currentRow)
                row.createCell(0).setCellValue(time!! + 0.00)
                row.createCell(1).setCellValue(underliningValue!! + 0.00)

                row.createCell(2).setCellValue(totalCallOiChange!! + 0.00)
                row.createCell(3).setCellValue(totalPutOiChange!! + 0.00)

//                row.createCell(4).setCellValue((totalPutOiChange!!-totalCallOiChange!!)+0.00)
                val totalOpenInterestChange = row.createCell(4)
                val totalChangeInOpenInterest = 0.00 + totalPutOiChange - totalCallOiChange

                totalOpenInterestChange.setCellValue(totalChangeInOpenInterest)
                val pcrCell = row.createCell(5)
                var pcrValue: Double = (totalPutOiChange + 0.00) / (totalCallOiChange + 0.00)
                if (pcrValue < 0) {
                    pcrValue *= -1
                }
                setStyle(pcrCell, pcrValue + 0.00)
                pcrCell.setCellValue(pcrValue + 0.00)
                row.createCell(6).setCellValue(oiData.value.marketDirectionVWAP!!)

                val tradeSignalCell = row.createCell(7)
                val isBuySignal = setStyle(totalOpenInterestChange, totalChangeInOpenInterest)
                if (isBuySignal) {
                    tradeSignalCell.setCellValue("BUY")
                    tradeSignal(tradeSignalCell, true)
                } else {
                    tradeSignalCell.setCellValue("SELL")
                    tradeSignal(tradeSignalCell, false)

                }

            }
        } else {
            defaultWorkSheet = workbook.getSheet(currentWorksheet)
            logger.debug("Retrieving Sheet: $currentWorksheet")
            val inputStream = FileInputStream(fileName)
            workbook = XSSFWorkbook(inputStream)
            val currentWorksheet = workbook.getSheet(currentWorksheet)
            //write OI Data
            var currentRow = 1
            for (oiData in oiDataMap) {
                val time = oiData.value.timeStamp
                val underliningValue = oiData.value.currentUnderlyingValue
                val totalCallOiChange = oiData.value.callChangeOI
                val totalPutOiChange = oiData.value.putchangeOI

                val row = currentWorksheet.createRow(currentRow)
                row.createCell(0).setCellValue(time!! + 0.00)
                row.createCell(1).setCellValue(underliningValue!! + 0.00)

                row.createCell(2).setCellValue(totalCallOiChange!! + 0.00)
                row.createCell(3).setCellValue(totalPutOiChange!! + 0.00)
                val totalOpenInterestChange = row.createCell(4)
                val totalChangeInOpenInterest = 0.00 + totalPutOiChange - totalCallOiChange
                totalOpenInterestChange.setCellValue(totalChangeInOpenInterest)
                val pcrCell = row.createCell(5)
                var pcrValue: Double = (totalPutOiChange + 0.00) / (totalCallOiChange + 0.00)
                if (pcrValue < 0) {
                    pcrValue *= -1
                }
                setStyle(pcrCell, pcrValue + 0.00)
                pcrCell.setCellValue(pcrValue + 0.00)
                row.createCell(6).setCellValue(oiData.value.marketDirectionVWAP!!)

                val tradeSignalCell = row.createCell(7)
                val isBuySignal = setStyle(totalOpenInterestChange, totalChangeInOpenInterest)
                if (isBuySignal) {
                    tradeSignalCell.setCellValue("BUY")
                    tradeSignal(tradeSignalCell, true)
                } else {
                    tradeSignalCell.setCellValue("SELL")
                    tradeSignal(tradeSignalCell, false)

                }
                if (oiDataMap.size > 3 && currentRow > 3) {
                    val updatedSignalCell = row.createCell(8)


                    val previousOiDifference =
                        oiDataMap[currentRow - 1]!!.putchangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!
                    logger.debug("OI Difference between two for row $currentRow : ${totalChangeInOpenInterest - previousOiDifference}")

                    //>50000
                    //diff 40K
                    when {

                        totalChangeInOpenInterest > 50000 -> {
                            if (totalChangeInOpenInterest - previousOiDifference > 10000) {
                                logger.debug(" oi difference between current and previous entry : ${totalChangeInOpenInterest - previousOiDifference}")
                                updatedSignalCell.setCellValue("BUY")
                                tradeSignal(updatedSignalCell, true)
                            }

                        }

                        totalChangeInOpenInterest < -50000 -> {
                            if (totalChangeInOpenInterest - previousOiDifference < -10000) {
                                logger.debug(" oi difference between current and previous entry : ${totalChangeInOpenInterest - previousOiDifference}")
                                updatedSignalCell.setCellValue("SELL")
                                tradeSignal(updatedSignalCell, false)
                            }
                        }

                    }
                }
                if (oiDataMap.size > 2 && currentRow > 2) {
                    val updatedSignalCell = row.createCell(9)
                    var averageCallOI = 0
                    var averagePutOI = 0
                    var avevrageOiDiff = 0

                    for (i in currentRow - 1 downTo currentRow - 2) {
                        averageCallOI = averageCallOI + oiDataMap[i]!!.callChangeOI!!
                        logger.debug("total call oi at index $i is ${oiDataMap[i]!!.callChangeOI}")
                        averagePutOI = averagePutOI + oiDataMap[i]!!.putchangeOI!!
                        logger.debug("total put oi at index $i is ${oiDataMap[i]!!.putchangeOI}")

                    }
                    averageCallOI /= 2
                    averagePutOI /= 2
                    avevrageOiDiff = averagePutOI - averageCallOI
                    logger.debug("average averageCallOI for row $currentRow : $averageCallOI, current call oi : ${oiDataMap[currentRow]!!.callChangeOI} ")
                    logger.debug("average averagePutOI for row $currentRow : $averagePutOI, current put oi : ${oiDataMap[currentRow]!!.putchangeOI} ")
                    logger.debug("average avevrageOiDiff for row $currentRow : $avevrageOiDiff, current put oi : ${totalChangeInOpenInterest}")

                    val previousOiDifference =
                        oiDataMap[currentRow - 1]!!.putchangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!
                    logger.debug("OI Difference between two for row $currentRow : ${totalChangeInOpenInterest - previousOiDifference}")


                    if (((oiDataMap[currentRow]!!.putchangeOI!! - oiDataMap[currentRow - 1]!!.putchangeOI!!) > 4000) &&(((oiDataMap[currentRow]!!.callChangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!) <-2000)) && (totalChangeInOpenInterest - previousOiDifference) < -10000) {
                        //&& ((totalChangeInOpenInterest-previousOiDifference) >5000)
//                        if (((oiDataMap[currentRow]!!.callChangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!) > 4000) && (((oiDataMap[currentRow]!!.putchangeOI!! - oiDataMap[currentRow - 1]!!.putchangeOI!!) < -3000)) && (totalChangeInOpenInterest - previousOiDifference) < -10000) {

                            logger.debug("SIGNAL BUY : current row $currentRow ,average averageCallOI : $averageCallOI, current call oi : ${oiDataMap[currentRow]!!.callChangeOI} ")
                        logger.debug("SIGNAL BUY :  current row $currentRow average averagePutOI : $averagePutOI, current put oi : ${oiDataMap[currentRow]!!.putchangeOI} ")
                        updatedSignalCell.setCellValue("BUY PUT")
                        tradeSignal(updatedSignalCell, false)
                    }

                    if (((oiDataMap[currentRow]!!.callChangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!) < -4000) &&(((oiDataMap[currentRow]!!.putchangeOI!! - oiDataMap[currentRow - 1]!!.putchangeOI!!)) > 2000)&& (totalChangeInOpenInterest - previousOiDifference) > 8000) {
//                        if (( && ((oiDataMap[currentRow]!!.callChangeOI!! - oiDataMap[currentRow - 1]!!.callChangeOI!!) < -3000) && (totalChangeInOpenInterest - previousOiDifference) > 10000) {

                            logger.debug("SIGNAL BUY : current row $currentRow ,average averageCallOI : $averageCallOI, current call oi : ${oiDataMap[currentRow]!!.callChangeOI} ")
                        logger.debug("SIGNAL BUY :  current row $currentRow average averagePutOI : $averagePutOI, current put oi : ${oiDataMap[currentRow]!!.putchangeOI} ")
                        updatedSignalCell.setCellValue("BUY CALL")
                        tradeSignal(updatedSignalCell, true)
                    }


                }
                currentRow += 1
            }
            commit(strikePrice.toInt())

        }
    }

    fun writeTradeSignal() {
        var totalRowCount = defaultWorkSheet.lastRowNum
        val previousRow = defaultWorkSheet.getRow(totalRowCount - 1)
        val currentRow: Row = defaultWorkSheet.getRow(totalRowCount)

        for (i in totalRowCount..1) {
            logger.debug("current row data at index $i is " + currentRow.getCell(0))
            logger.debug("current Total Call OI Change at index  $i is   " + currentRow.getCell(1))
            logger.debug("current Total Put OI Change at index  $i is  " + currentRow.getCell(2))
            logger.debug("current Total OI_DIFFERENCE at index  $i is  " + currentRow.getCell(3))

            logger.debug("previous row data at index $i is " + previousRow.getCell(0))
            logger.debug("previous Total Call OI Change at index  $i is   " + previousRow.getCell(1))
            logger.debug("previous Total Put OI Change at index  $i is  " + previousRow.getCell(2))
            logger.debug("previous Total OI_DIFFERENCE at index  $i is  " + previousRow.getCell(3))

        }

    }

}
