package com.example.datalayerexample

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.opencsv.CSVWriter
import java.io.*
import java.util.*


//This class is used to determine the name and version  of the database being used.
//It is responsible for opening the database if it exists, creating if it does not exist, and upgrading if required.

class DatabaseHandler(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "EmployeeDatabase"
        private const val TABLE_CONTACTS = "EmployeeTable"
        private const val KEY_ID = "_id"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
    }

    //is called if the database does not exist
    override fun onCreate(db: SQLiteDatabase?) {
        //creating table with fields
        //CREATE TABLE EmployeeTable(_id INTEGER PRIMARY KEY, name TEXT, email TEXT)
        val CREATE_CONTACTS_TABLE = ("CREATE TABLE " + TABLE_CONTACTS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_EMAIL + " TEXT" + ")")
        db?.execSQL(CREATE_CONTACTS_TABLE)
    }

    //is used to update the database schema to the most recent version
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    //CRUD Operations
    fun createEmployee(employee: EmployeeModel): Long {
        var db = this.writableDatabase

        val contentsValues = ContentValues()
        contentsValues.put(KEY_NAME, employee.name)
        contentsValues.put(KEY_EMAIL, employee.email)

        //Insert the row into the database
        var success = db.insert(TABLE_CONTACTS, null, contentsValues)

        //closes database connection
        db.close()

        //returns success value. Either true or false if it was successful or failed.
        return success
    }

    @SuppressLint("Range")
    fun readEmployee(): ArrayList<EmployeeModel> {
        val employeeList: ArrayList<EmployeeModel> = ArrayList<EmployeeModel>()

        //selecting all the rows that are in the contacts table
        val selectQuery = "SELECT * FROM $TABLE_CONTACTS"

        val db = this.readableDatabase
        var cursor: Cursor? = null

        //the cursor runs the query on the database
        try {
            cursor = db.rawQuery(selectQuery, null)
        } catch (e: SQLiteException) {
            db.execSQL(selectQuery)
            return ArrayList()
        }

        var id: Int
        var name: String
        var email: String

        if (cursor.moveToFirst()) {
            do {
                id = cursor.getInt(cursor.getColumnIndex(KEY_ID))
                name = cursor.getString(cursor.getColumnIndex(KEY_NAME))
                email = cursor.getString(cursor.getColumnIndex(KEY_EMAIL))

                val employee = EmployeeModel(id = id, name = name, email = email)
                employeeList.add(employee)
            } while (cursor.moveToNext())
        }
        return employeeList
    }

    fun updateEmployee(employee: EmployeeModel): Int {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(KEY_NAME, employee.name)
        contentValues.put(KEY_EMAIL, employee.email)

        //updating row
        val success = db.update(TABLE_CONTACTS, contentValues, KEY_ID + "=" + employee.id, null)

        db.close()
        return success
    }

    fun deleteEmployee(employee: EmployeeModel): Int {
        var db = this.writableDatabase
        var contentValues = ContentValues()
        contentValues.put(KEY_ID, employee.id)

        //deleting row
        val success = db.delete(TABLE_CONTACTS,KEY_ID + "=" + employee.id, null)
        db.close()
        return success
    }

    //Export the database file.
   // @SuppressLint("Range")
    @SuppressLint("Range")
    fun exportDB(context: Context): File {
        val exportDir = File(context.getExternalFilesDir(null), "com.example.datalayerexample")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "employeesDB.csv")
        try {
            file.createNewFile()
            val csvWrite = CSVWriter(FileWriter(file))
            val db = this.readableDatabase
            val curCSV = db.rawQuery("SELECT * FROM $TABLE_CONTACTS", null)

            // Write column names to CSV
            csvWrite.writeNext(curCSV.columnNames)

            // Write data to CSV
            while (curCSV.moveToNext()) {
                val id = curCSV.getString(curCSV.getColumnIndex(KEY_ID))
                val name = curCSV.getString(curCSV.getColumnIndex(KEY_NAME))
                val email = curCSV.getString(curCSV.getColumnIndex(KEY_EMAIL))

                // Which columns you want to export
                val arrStr = arrayOf(id, name, email)
                csvWrite.writeNext(arrStr)

            }
            Log.d("TAG", file.toString())
            csvWrite.close()
            curCSV.close()
            Log.d("TAG", "CSV file exported successfully")
        } catch (sqlEx: Exception) {
            Log.e("MainActivity", sqlEx.message, sqlEx)
        }
        return file
    }
}