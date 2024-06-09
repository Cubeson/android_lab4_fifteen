package com.example.zadanie4

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.gridlayout.widget.GridLayout
import kotlin.math.abs
import kotlin.random.Random


private const val gridSize = 4
private const val maxNumber = gridSize*gridSize

class GameActivity : AppCompatActivity() {

    private var markedTile : Tile? = null
    private lateinit var textViewInfo : TextView
    private lateinit var textViewCounter: TextView
    private var counter = 0
    private var mediaPlayer: MediaPlayer? = null

    class Tile(
        var imageView: ImageView,
        val col: Int,
        val row: Int,
        var number: Int){

        fun swap(other:Tile){
            val tempDrawable = other.imageView.drawable
            val tempNumber = other.number

            other.imageView.setImageDrawable(imageView.drawable)
            other.number = number

            imageView.setImageDrawable(tempDrawable)
            number = tempNumber
        }
        fun canSwap(other:Tile): Boolean{
            if(other.number != maxNumber)
                return false
            val x = abs(row-other.row) + abs(col-other.col)
            return x == 1
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val textViewTimeElapsed = findViewById<TextView>(R.id.textViewTimeElapsed)

        val t = Thread {
            val start = SystemClock.uptimeMillis()
            while(true){
                val elapsedMilliseconds = SystemClock.uptimeMillis() - start
                val elapsedSeconds = (elapsedMilliseconds/1000)

                val secondsDisplay = (elapsedSeconds % 60).toString()
                val minutesDisplay = (elapsedSeconds / 60).toString()
                val textDisplay = getString(R.string.time_elapsed_format,minutesDisplay,secondsDisplay)

                runOnUiThread {
                    textViewTimeElapsed.text = textDisplay
                }

                Thread.sleep(499)
            }

        }
        t.start()

        textViewInfo = findViewById<TextView>(R.id.textViewInfo)
        val textViewUsername = findViewById<TextView>(R.id.textViewUsername)
        textViewCounter = findViewById<TextView>(R.id.textViewCounter)
        textViewCounter.text = counter.toString()
        textViewUsername.text = OptionsActivity.username
        val buttonBack = findViewById<Button>(R.id.buttonBack)
        buttonBack.setOnClickListener{
            finish()
        }

        val gridLayout = findViewById<GridLayout>(R.id.gridLayout0)
        val imageViewMovement = findViewById<ImageView>(R.id.imageViewMovement)
        val puzzleGrid = initializeBoard(gridLayout,imageViewMovement)
        shuffle(puzzleGrid)
    }


    private fun initializeBoard(gridLayout: GridLayout, imageViewMovement: ImageView): Array<Array<Tile>> {
        val puzzleGrid = Array(gridSize) {Array(gridSize) { Tile(gridLayout[0] as ImageView,0,0,0) } }
        var index = 0
        for(y in 0..<gridSize){
            for(x in 0..<gridSize){
                val tile = Tile(gridLayout[index] as ImageView,y,x,index+1)
                puzzleGrid[y][x] = tile
                tile.imageView.setOnClickListener(tileOnClickFunction(tile, puzzleGrid, imageViewMovement))
                index++
            }
        }
        return puzzleGrid
    }

    private fun tileOnClickFunction(clickedTile: Tile, puzzleGrid: Array<Array<Tile>>, imageViewMovement : ImageView): (v: View) -> Unit =
        setOnClickListener@{

            if(OptionsActivity.clickTwice){
                if(markedTile == null){ // select
                    if(clickedTile.number == maxNumber)
                        return@setOnClickListener
                    markedTile = clickedTile
                    markedTile!!.imageView.setColorFilter(Color.argb(64,0,255,0))
                    return@setOnClickListener
                }
                if(clickedTile == markedTile){ // unselect
                    markedTile = null
                    clickedTile.imageView.setColorFilter(Color.argb(0,0,0,0))
                    return@setOnClickListener
                }else{ // swap tiles
                    if(!markedTile!!.canSwap(clickedTile))
                        return@setOnClickListener
                    playSound(R.raw.swoosh)
                    animateImageView(imageViewMovement, clickedTile, puzzleGrid)
                    counter++
                    textViewCounter.text = counter.toString()
                }
                return@setOnClickListener
            }
            // else (click once)
            val emptyTile = puzzleGrid.flatten().find { tile -> tile.number == maxNumber }!!
            if(clickedTile.canSwap(emptyTile)){
                clickedTile.imageView.setColorFilter(Color.argb(64,0,255,0))
                markedTile = clickedTile
                playSound(R.raw.swoosh)
                animateImageView(imageViewMovement, emptyTile,puzzleGrid)
                counter++
                textViewCounter.text = counter.toString()
            }

        }

    private fun animateImageView(
        imageViewMovement: ImageView,
        tile: Tile,
        puzzleGrid: Array<Array<Tile>>
    ) {
        imageViewMovement.setImageDrawable(markedTile!!.imageView.drawable)

        val locStart = IntArray(2)
        markedTile!!.imageView.getLocationOnScreen(locStart)
        val xStart = locStart[0].toFloat()
        val yStart = locStart[1].toFloat()

        val locEnd = IntArray(2)
        tile.imageView.getLocationOnScreen(locEnd)
        val xEnd = locEnd[0].toFloat()
        val yEnd = locEnd[1].toFloat()

        imageViewMovement.x = xStart
        imageViewMovement.y = yStart
        imageViewMovement.visibility = View.VISIBLE
        tile.imageView.visibility = View.INVISIBLE
        markedTile!!.imageView.visibility = View.INVISIBLE

        imageViewMovement.animate()
            .x(xEnd)
            .y(yEnd)
            .setDuration(250)
            .withEndAction {
                imageViewMovement.visibility = View.INVISIBLE
                tile.imageView.visibility = View.VISIBLE
                markedTile!!.imageView.visibility = View.VISIBLE

                markedTile!!.swap(tile)
                markedTile!!.imageView.setColorFilter(Color.argb(0, 0, 0, 0))
                tile.imageView.setColorFilter(Color.argb(0, 0, 0, 0))
                markedTile = null
                if (isPuzzleSolved(puzzleGrid)) {
                    textViewInfo.text = "You solved the puzzle!"
                }
            }
    }

    private fun isSolvable(puzzleGrid: Array<Array<Tile>>): Boolean{
        var parity = 0
        var row = 0
        val flatGrid = puzzleGrid.flatten()
        val emptyTile = flatGrid.find { tile -> tile.number == maxNumber }!!
        for(i in 0..<maxNumber){
            if(i % gridSize == 0)
                row += 1
            for(j in i+1..<maxNumber){
                if(flatGrid[i].number > flatGrid[j].number && flatGrid[j].number != maxNumber)
                    parity += 1
            }
        }

        if(emptyTile.row % 2 == 0){
            return parity % 2 == 0
        }
        return parity % 2 != 0
    }

    private fun shuffle(puzzleGrid:Array<Array<Tile>>){
        val moves = 50
        val emptyTile = puzzleGrid.flatten().find { tile -> tile.number == maxNumber }!!
        do{
            for(i in 0..<moves){
                val col = Random.nextInt(0, gridSize)
                val row = Random.nextInt(0, gridSize)
                emptyTile.swap(puzzleGrid[col][row])
            }
        }while (!isSolvable(puzzleGrid) || isPuzzleSolved(puzzleGrid))
    }
    private fun isPuzzleSolved(puzzleGrid: Array<Array<Tile>>) : Boolean{
        // from 1 to 15
        var currentNumber = -1
        for (col in 0..<gridSize)
            for(row in 0..<gridSize){
                if(puzzleGrid[col][row].number < currentNumber)
                    return false
                currentNumber = puzzleGrid[col][row].number
            }
        return true;
    }

    private fun playSound(resId : Int){
        mediaPlayer?.reset()
        mediaPlayer = MediaPlayer.create(this,resId)
        mediaPlayer!!.start()
    }
}