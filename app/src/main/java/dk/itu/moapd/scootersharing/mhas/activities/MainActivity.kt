package dk.itu.moapd.scootersharing.mhas.activities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import dk.itu.moapd.scootersharing.mhas.R

class MainActivity : AppCompatActivity() {

    /**
     * Creates the activity
     * Sets the content of the activity to be the FragmentContainerView which holds the relevant fragments defined
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

    }

}


