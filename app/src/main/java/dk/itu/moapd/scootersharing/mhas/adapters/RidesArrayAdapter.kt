package dk.itu.moapd.scootersharing.mhas.adapters
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import dk.itu.moapd.scootersharing.mhas.databinding.ListRidesBinding
import dk.itu.moapd.scootersharing.mhas.models.Scooter
import java.text.SimpleDateFormat
import java.util.*

class RidesArrayAdapter(private val context : Context, private val scooters : List<Scooter>) : RecyclerView.Adapter<RidesArrayAdapter.RideHolder>() {

     private lateinit var mListener : OnItemClickListener

     interface OnItemClickListener {
         fun onItemClick(position: Int)
     }


     fun setOnItemClickListener(listener: OnItemClickListener){
        mListener = listener
     }

     inner class RideHolder(private val binding : ListRidesBinding, listener : OnItemClickListener) : RecyclerView.ViewHolder(binding.root) {

         init {
            itemView.setOnClickListener {
                listener.onItemClick(absoluteAdapterPosition)
            }
         }

         fun bind(scooter: Scooter) {
            binding.scooterName.text = scooter.name
            binding.scooterLocation.text = scooter.location
            binding.scooterTimestamp.text = convertLongToTime(scooter.timestamp)

            val storage = Firebase.storage
            val imageRef = storage.reference.child(scooter.url)

            imageRef.downloadUrl.addOnSuccessListener {
                Glide.with(context)
                    .load(it)
                    .into(binding.scooterImage)
            }

         }

         private fun convertLongToTime(time: Long): String {
             val date = Date(time)
             val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
             return format.format(date)
         }

     }

    override fun onCreateViewHolder(parent: ViewGroup, viewType : Int): RideHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = ListRidesBinding.inflate(inflater,parent,false)

        return RideHolder(binding,mListener)

    }

    override fun onBindViewHolder(holder: RideHolder, position: Int) {
        val scooter = scooters[position]

        holder.bind(scooter)

    }

    override fun getItemCount() = scooters.size


}