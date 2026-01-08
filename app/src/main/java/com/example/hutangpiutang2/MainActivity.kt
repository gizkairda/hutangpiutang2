package com.example.hutangpiutang2

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hutangpiutang2.databinding.ActivityMainBinding
import com.example.hutangpiutang2.databinding.DialogAddEditDebtBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var debtAdapter: DebtAdapter
    private val debts = mutableListOf<Debt>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        // Cek apakah user sudah login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupRecyclerView()
        setupClickListeners()
        loadDebts()
    }

    private fun setupRecyclerView() {
        debtAdapter = DebtAdapter(
            debts = debts,
            onEditClick = { debt ->
                showEditDialog(debt)
            },
            onDeleteClick = { debt ->
                showDeleteDialog(debt)
            },
            onStatusClick = { debt ->
                toggleStatus(debt)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = debtAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnFilterAktif.setOnClickListener {
            loadDebtsByStatus("aktif")
        }

        binding.btnFilterLunas.setOnClickListener {
            loadDebtsByStatus("lunas")
        }

        binding.btnFilterAll.setOnClickListener {
            loadDebts()
        }
    }

    // CREATE: Load data dari Firebase
    private fun loadDebts() {
        val userId = auth.currentUser?.uid ?: return

        database.child("debts").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    debts.clear()
                    for (childSnapshot in snapshot.children) {
                        val debt = childSnapshot.getValue(Debt::class.java)
                        debt?.id = childSnapshot.key ?: ""
                        if (debt != null) {
                            debts.add(debt)
                        }
                    }
                    debtAdapter.updateList(debts)
                    calculateTotals()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadDebtsByStatus(status: String) {
        val userId = auth.currentUser?.uid ?: return

        database.child("debts").child(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    debts.clear()
                    for (childSnapshot in snapshot.children) {
                        val debt = childSnapshot.getValue(Debt::class.java)
                        if (debt?.status == status) {
                            debt.id = childSnapshot.key ?: ""
                            debts.add(debt)
                        }
                    }
                    debtAdapter.updateList(debts)
                    calculateTotals()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun calculateTotals() {
        var totalHutang = 0.0
        var totalPiutang = 0.0

        for (debt in debts) {
            if (debt.status == "aktif") {
                if (debt.type == "hutang") {
                    totalHutang += debt.amount
                } else {
                    totalPiutang += debt.amount
                }
            }
        }

        val balance = totalPiutang - totalHutang

        binding.tvTotalHutang.text = "Total Hutang: Rp ${String.format("%,.0f", totalHutang)}"
        binding.tvTotalPiutang.text = "Total Piutang: Rp ${String.format("%,.0f", totalPiutang)}"
        binding.tvBalance.text = "Saldo: Rp ${String.format("%,.0f", balance)}"

        if (balance >= 0) {
            binding.tvBalance.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvBalance.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    // CREATE: Tambah data baru
    private fun showAddDialog() {
        val dialogBinding = DialogAddEditDebtBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Tambah Hutang/Piutang")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val amount = dialogBinding.etAmount.text.toString()
                val personName = dialogBinding.etPersonName.text.toString()
                val type = if (dialogBinding.rbHutang.isChecked) "hutang" else "piutang"

                if (title.isEmpty() || amount.isEmpty() || personName.isEmpty()) {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amountDouble = try {
                    amount.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Jumlah harus angka", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                saveDebt(title, amountDouble, personName, type)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveDebt(title: String, amount: Double, personName: String, type: String) {
        val userId = auth.currentUser?.uid ?: return

        val debt = Debt(
            title = title,
            amount = amount,
            personName = personName,
            type = type,
            status = "aktif",
            userId = userId
        )

        val debtRef = database.child("debts").child(userId).push()
        debt.id = debtRef.key ?: ""

        debtRef.setValue(debt)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // UPDATE: Edit data
    private fun showEditDialog(debt: Debt) {
        val dialogBinding = DialogAddEditDebtBinding.inflate(layoutInflater)

        dialogBinding.etTitle.setText(debt.title)
        dialogBinding.etAmount.setText(debt.amount.toString())
        dialogBinding.etPersonName.setText(debt.personName)
        if (debt.type == "hutang") {
            dialogBinding.rbHutang.isChecked = true
        } else {
            dialogBinding.rbPiutang.isChecked = true
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Hutang/Piutang")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val title = dialogBinding.etTitle.text.toString()
                val amount = dialogBinding.etAmount.text.toString()
                val personName = dialogBinding.etPersonName.text.toString()
                val type = if (dialogBinding.rbHutang.isChecked) "hutang" else "piutang"

                if (title.isEmpty() || amount.isEmpty() || personName.isEmpty()) {
                    Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amountDouble = try {
                    amount.toDouble()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Jumlah harus angka", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                updateDebt(debt.id, title, amountDouble, personName, type)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateDebt(debtId: String, title: String, amount: Double, personName: String, type: String) {
        val userId = auth.currentUser?.uid ?: return

        val debtUpdates = hashMapOf<String, Any>(
            "title" to title,
            "amount" to amount,
            "personName" to personName,
            "type" to type
        )

        database.child("debts").child(userId).child(debtId)
            .updateChildren(debtUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // DELETE: Hapus data
    private fun showDeleteDialog(debt: Debt) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Data")
            .setMessage("Yakin hapus '${debt.title}'?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteDebt(debt.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteDebt(debtId: String) {
        val userId = auth.currentUser?.uid ?: return

        database.child("debts").child(userId).child(debtId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal hapus: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Ubah status (Aktif/Lunas)
    private fun toggleStatus(debt: Debt) {
        val newStatus = if (debt.status == "aktif") "lunas" else "aktif"
        val userId = auth.currentUser?.uid ?: return

        database.child("debts").child(userId).child(debt.id)
            .child("status")
            .setValue(newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Status berhasil diubah", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal ubah status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}