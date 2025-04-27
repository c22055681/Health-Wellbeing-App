package com.example.healthandwellbeingapp.goals
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthandwellbeingapp.R
import com.example.healthandwellbeingapp.goals.Goal



class GoalAdapter(
    private val goals: MutableList<Goal>,
    private val onUpdate: (Goal) -> Unit,
    private val onDelete: (Goal) -> Unit
) : RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    inner class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDetails: TextView = view.findViewById(R.id.txtGoalDetails)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnUpdate: Button = view.findViewById(R.id.btnUpdate)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.txtDetails.text = "${goal.name} - ${goal.currentValue}/${goal.targetValue} (Daily: ${goal.dailyImprovement})"
        holder.progressBar.max = (goal.targetValue - goal.startValue).toInt()
        holder.progressBar.progress = (goal.currentValue - goal.startValue).toInt()

        holder.btnUpdate.setOnClickListener { onUpdate(goal) }
        holder.btnDelete.setOnClickListener { onDelete(goal) }
    }

    override fun getItemCount() = goals.size
}
