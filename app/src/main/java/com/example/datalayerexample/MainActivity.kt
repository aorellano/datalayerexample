package com.example.datalayerexample


import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*


class MainActivity : AppCompatActivity() {
    //variable setup
    lateinit var addButton: Button
    lateinit var nameTextField: EditText
    lateinit var emailTextField: EditText
    lateinit var recyclerViewList: RecyclerView
    lateinit var tvNoRecordsAvailable: TextView
    lateinit var tvUpdate:TextView
    lateinit var exportButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Asks user for permission to send db file through
        askForUserPermission()
        setupUI()
        setupListDataIntoRecyclerView()
    }

    fun setupUI() {
        setContentView(R.layout.activity_main)
        addButton = findViewById(R.id.btnAdd)
        nameTextField = findViewById(R.id.etName)
        emailTextField = findViewById(R.id.etEmailId)
        recyclerViewList = findViewById(R.id.rvItemsList)
        tvNoRecordsAvailable = findViewById(R.id.tvNoRecordsAvailable)
        exportButton = findViewById(R.id.btnExport)
        addButtonListeners()
    }

    private fun addButtonListeners() {
        //once the add record button is pressed it will add the name and email to the database
        addButton.setOnClickListener { view ->
            createRecord(view)
        }

        //once the export database file it will give options where to send the file
        exportButton.setOnClickListener { view ->
            sendDBFile()
        }
    }

    private fun sendDBFile() {
        val databaseHandler: DatabaseHandler = DatabaseHandler(this)
        val emailIntent = Intent(Intent.ACTION_SEND)
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.type = "text/csv"

        val file = databaseHandler.exportDB(this)
        val fileUri: Uri = FileProvider.getUriForFile(
            this,
            "com.example.datalayerexample.fileprovider",
            file
        )
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

        // Set email subject and body
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "CSV File Attachment")
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Please find the attached CSV file.")
        emailIntent.setDataAndType(fileUri, "application/csv");

        // Start the email intent: Will present the options screen at the bottom
        startActivity(Intent.createChooser(emailIntent, "Send email..."))
    }

    private fun createRecord(view: View) {
        val name = nameTextField.text.toString()
        val email = emailTextField.text.toString()
        val databaseHandler: DatabaseHandler = DatabaseHandler(this)

        if (name != "" && email != "") {
            val status = databaseHandler.createEmployee(EmployeeModel(0, name, email))
            if (status > -1) {
                val toast2 = Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT)
                toast2.show()
                nameTextField.text.clear()
                emailTextField.text.clear()
                setupListDataIntoRecyclerView()
            }
        } else {
            val toast1 = Toast.makeText(this, "Field Cannot Be Empty", Toast.LENGTH_SHORT)
            toast1.show()
        }
    }

    fun updateRecord(employeeModel: EmployeeModel) {
        val updateDialog = Dialog(this, R.style.Theme_Dialog)
        updateDialog.setCancelable(false)
        updateDialog.setContentView(R.layout.dialog_update)
        val etUpdateName = updateDialog.findViewById<EditText>(R.id.etUpdateName)
        val etUpdateEmailId = updateDialog.findViewById<EditText>(R.id.etUpdateEmailId)
        val tvCancel = updateDialog.findViewById<TextView>(R.id.tvCancel)
        etUpdateName.setText(employeeModel.name)
        etUpdateEmailId.setText(employeeModel.email)
        tvUpdate = updateDialog.findViewById<TextView>(R.id.tvUpdate)

        tvUpdate.setOnClickListener(View.OnClickListener {
            val name = etUpdateName.text.toString()
            val email = etUpdateEmailId.text.toString()
            val databaseHandler: DatabaseHandler = DatabaseHandler(this)

            if (name != "" && email != "") {
                val status =
                    databaseHandler.updateEmployee(EmployeeModel(employeeModel.id, name, email))
                if (status > -1) {
                    Toast.makeText(applicationContext, "Record Updated.", Toast.LENGTH_LONG).show()
                    setupListDataIntoRecyclerView()
                    updateDialog.dismiss() // Dialog will be dismissed
               }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Name or Email cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
        tvCancel.setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })
        //Start the dialog and display it on screen.
        updateDialog.show()
    }

    private fun getItemsList(): ArrayList<EmployeeModel> {
        val databaseHandler: DatabaseHandler = DatabaseHandler(this)
        return databaseHandler.readEmployee()
    }

    private fun setupListDataIntoRecyclerView() {
        if (getItemsList().size > 0) {
            recyclerViewList.visibility = View.VISIBLE
            tvNoRecordsAvailable.visibility = View.GONE
            recyclerViewList.layoutManager = LinearLayoutManager(this)
            val itemAdapter = ItemAdapter(this, getItemsList())
            recyclerViewList.adapter = itemAdapter
        } else {
            recyclerViewList.visibility = View.GONE
            tvNoRecordsAvailable.visibility = View.VISIBLE
        }
    }

    /**
     * Method is used to show the delete alert dialog.
     */
    fun deleteRecord(employeeModel: EmployeeModel) {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle("Delete Record")
        //set message for alert dialog
        builder.setMessage("Are you sure you wants to delete ${employeeModel.name}.")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        //performing positive action
        builder.setPositiveButton("Yes") { dialogInterface, which ->

            //creating the instance of DatabaseHandler class
            val databaseHandler: DatabaseHandler = DatabaseHandler(this)
            //calling the deleteEmployee method of DatabaseHandler class to delete record
            val status = databaseHandler.deleteEmployee(EmployeeModel(employeeModel.id, "", ""))
            if (status > -1) {
                Toast.makeText(
                    applicationContext,
                    "Record deleted successfully.",
                    Toast.LENGTH_LONG
                ).show()

                setupListDataIntoRecyclerView()
            }

            dialogInterface.dismiss() // Dialog will be dismissed
        }
        //performing negative action
        builder.setNegativeButton("No") { dialogInterface, which ->
            dialogInterface.dismiss() // Dialog will be dismissed
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false) // Will not allow user to cancel after clicking on remaining screen area.
        alertDialog.show()  // show the dialog to UI
    }

    private fun askForUserPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
        }
    }

    companion object {
        private const val REQUEST_WRITE_STORAGE = 123
    }
}