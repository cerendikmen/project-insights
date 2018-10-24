package utilities


import java.io.{BufferedWriter, FileWriter}

import com.opencsv._
import play.api.Logger
import smile.validation.mutualInformationScore

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.collection.mutable.{ArrayBuffer, HashMap, ListBuffer}
import scala.io.Source
import scala.util.{Try, _}

/**
  * Handles CSV reading
  */
object CsvReader {
  val csvLogger: Logger = Logger("CsvReader")

  /** Reads the CSV file
    *
    * Read 1st row as list of variable names.
    * Read the rest as samples.
    * Drop first item of every row, namely "id".
    * Put skipValue for every not known or missing variable state.
    *
    * @return (List of variables, Int[][] samples (with skipValue))
    */
  def readCsvFile(csvPath: String, nrows: Int, ncols: Int, skipValue: Int) = {
    csvLogger.info(s"Reading CSV file from the given path: $csvPath")
    val bufferedSource = Source.fromFile(csvPath)
    val iter: Iterator[String] = bufferedSource.getLines

    // Read variables on the 1st row
    val variables: Array[String] = iter.next().split(",").drop(1).map(_.trim)

    // Read samples
    val rows = Array.ofDim[Int](nrows, ncols)
    var count: Int = 0
    iter.foreach { line =>
      rows(count) = line.split(",", -1).drop(1).map(_.trim).map(toInt(_, skipValue))
      count += 1
    }
    bufferedSource.close
    (variables, rows.transpose)
  }

  /** String to Int conversion , returns the skipValue
    * if the string can not be converted to an int.
    * skipValue will be used for empty fields in the CSV.
    *
    * @return Int
    */
  private def toInt(s: String, skipValue: Int): Int = {
    try {
      s.toInt
    } catch {
      case e: Exception => skipValue
    }
  }
}

/**
  * Main point for the controller to read CSV file and
  * calculate mutual information
  *
  * Parses a property matrix (from CSV) into memory
  */
object DataUtility {
  val dataLogger: Logger = Logger("DataUtility")

  private val DEFAULT_CSV: String = "resources/dataset.csv"
  private val NUM_OF_ROWS: Int = 36 // number of samples
  private val NUM_OF_COLS: Int = 30 // number of variables
  private val SKIP_FLAG: Int = 2 // used to mark not known or missing variables
  val CSV_FILE: String = "result.csv"

  // CSV content will be stored into memory after the first access and read
  private var variables: Option[Array[String]] = None
  private var samples: Option[Array[Array[Int]]] = None

  /** Reads the CSV file from the disk only once
    * then stores the content into memory
    * for further usages.
    *
    * @return Tuple of (List of variable names, 2d array of samples)
    */
  def parseFromCsv = {
    if (variables.isEmpty || samples.isEmpty) {
      dataLogger.info("Data not found, reading from CSV")
      val (names, data) = CsvReader.readCsvFile(DEFAULT_CSV, NUM_OF_ROWS, NUM_OF_COLS, SKIP_FLAG)
      variables = Some(names)
      samples = Some(data)
    }

    (variables.getOrElse(Array[String]()), samples.getOrElse(Array.ofDim[Int](NUM_OF_ROWS, NUM_OF_COLS)))
  }

  /** Returns the variable names read from CSV */
  def getVariableList = {
    parseFromCsv._1
  }

  /** Calculates mutual information
    *
    *
    * @param variable The given variable name from CSV
    * @return Map( variable name -> mutual information)
    */
  def getMI(variable: String) = {
    dataLogger.info(s"Getting MI for $variable")

    val (variables, data) = parseFromCsv
    val target = variables.indexOf(variable)


    val result: HashMap[Double, String]  = HashMap()

    // Get mutual information between the variable given and the other variables respectively
    for (curr <- data.indices.filter(_ != target)) {
      dataLogger.info(s" mutual information with : ${variables(curr)}")

      val (clean1, clean2) = preProcess(data(target), data(curr))
      val mi = calculateMI(clean1, clean2)
      result += (mi -> variables(curr))
    }

    val sortedMap = ListMap(result.toSeq.sortWith(_._1 > _._1):_*)
    var rows = new ListBuffer[List[String]]()

    sortedMap foreach (x => {
      val row: List[String] = List(x._1.toString, x._2)
      rows += row
    })

    val header: List[String] = List("mutual_info", "variable")
    CsvWriter.writeCsvFile(CSV_FILE, header, rows.toList)
  }

  /** Prepare two arrays for calculation
    * by removing any pair if one of them
    * has a value SKIP_FLAG.
    *
    * Empty values were marked while
    * reading CSV file. Such values will be
    * removed before calculations on a
    * pair-wise basis.
    *
    * This might result in loosing some amount of information when calculating
    * p(A) or p(B).
    *
    * @param rawData1 Row with possible invalid values
    * @param rawData2 Row with possible invalid values
    * @return Tuple2 of Int Arrays without invalid values
    */
  private def preProcess(rawData1: Array[Int], rawData2: Array[Int]) = {
    val v1 = ArrayBuffer[Int]()
    val v2 = ArrayBuffer[Int]()

    for (i <- rawData1.indices) {
      if (rawData1(i) != SKIP_FLAG && rawData2(i) != SKIP_FLAG) {
        v1 += rawData1(i)
        v2 += rawData2(i)
      }
    }
    (v1.toArray, v2.toArray)
  }

  /** Calculates Mutual Information between 2 variables
    *
    *
    * smile used as a library to prevent any calculation errors
    * that might be occurred.
    *
    * @param v1 First array
    * @param v2 Second array
    * @return MI as a Double value
    */
  def calculateMI(v1: Array[Int], v2: Array[Int]): Double = {
    val res = mutualInformationScore(v1, v2)

    res
  }
}

/**
  * Handles CSV writing
  */
object CsvWriter {
  val csvLogger: Logger = Logger("CsvWriter")

  def writeCsvFile(fileName: String, header: List[String], rows: List[List[String]]): Try[Unit] =
    Try(new CSVWriter(new BufferedWriter(new FileWriter(fileName)))).flatMap((csvWriter: CSVWriter) =>
      Try{
        csvLogger.info(s"Writing CSV file with the given name: $fileName")
        csvWriter.writeAll(
          (header +: rows).map(_.toArray).asJava
        )
        csvWriter.close()
      } match {
        case f @ Failure(_) =>
          // Always return the original failure.  In production code we might
          // define a new exception which wraps both exceptions in the case
          // they both fail, but that is omitted here.
          Try(csvWriter.close()).recoverWith{
            case _ => f
          }
        case success =>
          success
      }
    )
}