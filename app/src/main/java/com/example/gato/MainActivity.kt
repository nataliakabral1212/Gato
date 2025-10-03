package com.example.gato

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var etPlayer1: EditText
    private lateinit var etPlayer2: EditText
    private lateinit var btnStart: Button
    private lateinit var btnRendirse: Button
    private lateinit var etTurn: EditText
    private lateinit var tvPlayerO: TextView
    private lateinit var tvPlayerX: TextView
    private lateinit var tvRendirseCounter: TextView

    private val board = Array(9) { "" }
    private lateinit var buttons: Array<Button>

    private var player1Name = ""
    private var player2Name = ""
    private var playerOName = ""
    private var playerXName = ""

    private var currentPlayer = "O"
    private var rendirseCount = 0
    private var lastRendirseTimestamp = 0L
    private var gameStarted = false

    private val RENDIRSE_MAX_INTERVAL = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPlayer1 = findViewById(R.id.etPlayer1)
        etPlayer2 = findViewById(R.id.etPlayer2)
        btnStart = findViewById(R.id.btnStart)
        btnRendirse = findViewById(R.id.btnRendirse)
        etTurn = findViewById(R.id.etTurn)
        tvPlayerO = findViewById(R.id.tvPlayerO)
        tvPlayerX = findViewById(R.id.tvPlayerX)
        tvRendirseCounter = findViewById(R.id.tvRendirseCounter)

        buttons = arrayOf(
            findViewById(R.id.btn0),
            findViewById(R.id.btn1),
            findViewById(R.id.btn2),
            findViewById(R.id.btn3),
            findViewById(R.id.btn4),
            findViewById(R.id.btn5),
            findViewById(R.id.btn6),
            findViewById(R.id.btn7),
            findViewById(R.id.btn8)
        )

        val prefs = getSharedPreferences("TicTacToe", Context.MODE_PRIVATE)
        etPlayer1.setText(prefs.getString("player1", ""))
        etPlayer2.setText(prefs.getString("player2", ""))

        buttons.forEach { it.isEnabled = true; it.text = "" }
        etTurn.setText("")
        etTurn.isFocusable = false
        etTurn.isFocusableInTouchMode = false
        etTurn.isClickable = false

        tvRendirseCounter.visibility = View.GONE

        etPlayer1.setText("")
        etPlayer2.setText("")

        btnStart.setOnClickListener {
            player1Name = etPlayer1.text.toString().trim()
            player2Name = etPlayer2.text.toString().trim()

            if (player1Name.isEmpty() || player2Name.isEmpty()) {
                Toast.makeText(this, "El juego no ha empezado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (player1Name.equals(player2Name, ignoreCase = true)) {
                Toast.makeText(this, "Los nombres no pueden ser iguales", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                Toast.makeText(this, "Partida iniciada", Toast.LENGTH_SHORT).show()
            }

            prefs.edit().apply {
                putString("player1", player1Name)
                putString("player2", player2Name)
                apply()
            }

            playerOName = player1Name
            playerXName = player2Name

            tvPlayerO.text = "O: $playerOName"
            tvPlayerX.text = "X: $playerXName"

            etPlayer1.isEnabled = false
            etPlayer2.isEnabled = false

            startGame()
        }

        btnRendirse.setOnClickListener {
            if (!gameStarted) {
                Toast.makeText(this, "No hay partida en curso", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            if (now - lastRendirseTimestamp <= RENDIRSE_MAX_INTERVAL) {
                rendirseCount++
            } else {
                rendirseCount = 1
            }
            lastRendirseTimestamp = now

            if (rendirseCount < 5) {
                val remaining = 5 - rendirseCount
                tvRendirseCounter.text = "¡Rendirse! Faltan $remaining clics ($rendirseCount/5)"
                tvRendirseCounter.visibility = View.VISIBLE

            } else {
                tvRendirseCounter.visibility = View.GONE

                val loser = getPlayerName(currentPlayer)
                val winnerSymbol = if (currentPlayer == "O") "X" else "O"
                val winner = getPlayerName(winnerSymbol)

                Toast.makeText(this, "Ganador: $winner", Toast.LENGTH_LONG).show()
                Toast.makeText(this, "Jugador $loser se rindió", Toast.LENGTH_SHORT).show()
                resetGame()
            }
        }

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                makeMove(index, button)
            }
        }
    }

    private fun startGame() {
        gameStarted = true
        rendirseCount = 0
        lastRendirseTimestamp = 0L
        tvRendirseCounter.visibility = View.GONE

        for (i in board.indices) {
            board[i] = ""
            buttons[i].text = ""
            buttons[i].isEnabled = true
        }

        playerOName = player1Name
        playerXName = player2Name

        tvPlayerO.text = "O: $playerOName"
        tvPlayerX.text = "X: $playerXName"

        currentPlayer = if (Random.nextBoolean()) "O" else "X"
        etTurn.setText(getPlayerName(currentPlayer))
    }

    private fun makeMove(index: Int, button: Button) {
        if (!gameStarted) {
            Toast.makeText(this, "La partida no ha iniciado", Toast.LENGTH_SHORT).show()
            return
        }
        if (board[index].isNotEmpty()) {
            Toast.makeText(this, "Esa casilla ya está ocupada", Toast.LENGTH_SHORT).show()
            return
        }

        board[index] = currentPlayer
        button.text = currentPlayer

        rendirseCount = 0
        lastRendirseTimestamp = 0L
        tvRendirseCounter.visibility = View.GONE

        if (checkWinner()) {
            val winnerName = getPlayerName(currentPlayer)
            Toast.makeText(this, "Ganador: $winnerName", Toast.LENGTH_LONG).show()
            Toast.makeText(this, "Felicidades $winnerName 🎉", Toast.LENGTH_SHORT).show()
            resetGame()
            return
        }

        if (board.all { it.isNotEmpty() }) {
            Toast.makeText(this, "Empate 🤝", Toast.LENGTH_SHORT).show()
            resetGame()
            return
        }

        currentPlayer = if (currentPlayer == "O") "X" else "O"
        etTurn.setText(getPlayerName(currentPlayer))
    }

    private fun checkWinner(): Boolean {
        val winPatterns = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        return winPatterns.any { (a, b, c) ->
            board[a] == currentPlayer && board[b] == currentPlayer && board[c] == currentPlayer
        }
    }

    private fun resetGame() {
        gameStarted = false
        rendirseCount = 0
        lastRendirseTimestamp = 0L
        tvRendirseCounter.visibility = View.GONE
        board.fill("")
        buttons.forEach { it.text = "" }
        etTurn.setText("")

        tvPlayerO.text = "Jugador O"
        tvPlayerX.text = "Jugador X"

        etPlayer1.isEnabled = true
        etPlayer2.isEnabled = true

        etPlayer1.setText("")
        etPlayer2.setText("")

        val prefs = getSharedPreferences("TicTacToe", Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove("player1")
            remove("player2")
            apply()
        }
    }

    private fun getPlayerName(symbol: String): String {
        return if (symbol == "O") playerOName else playerXName
    }
}
