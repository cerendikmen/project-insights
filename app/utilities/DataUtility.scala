package utilities


import play.api.Logger

import scala.io.Source


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
}