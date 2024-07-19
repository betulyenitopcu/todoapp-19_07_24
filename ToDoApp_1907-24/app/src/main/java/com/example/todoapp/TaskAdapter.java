package com.example.todoapp;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final Context context;

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.taskTextView.setText(task.getTaskText());
        holder.checkBox.setChecked(task.isChecked());

        holder.checkBox.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                boolean isChecked = holder.checkBox.isChecked();
                clickedTask.setChecked(isChecked);

                DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                        .child("tasks").child(clickedTask.getUserId()).child(clickedTask.getId());
                tasksRef.child("checked").setValue(isChecked)
                        .addOnSuccessListener(aVoid -> Log.d("FirebaseUpdate", "Task isChecked updated successfully"))
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseUpdate", "Failed to update isChecked", e);
                            Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        holder.editButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                showEditDialog(clickedTask);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Task clickedTask = taskList.get(adapterPosition);
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                Log.d("TaskAdapter", "Clicked Task User ID: " + clickedTask.getUserId());
                Log.d("TaskAdapter", "Current User ID: " + currentUserId);

                if (clickedTask.getUserId().equals(currentUserId)) {
                    DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                            .child("tasks").child(clickedTask.getUserId()).child(clickedTask.getId());
                    tasksRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("FirebaseDelete", "Task deleted successfully");
                                if (adapterPosition >= 0 && adapterPosition < taskList.size()) {
                                    taskList.remove(adapterPosition);
                                    notifyItemRemoved(adapterPosition);
                                    notifyItemRangeChanged(adapterPosition, taskList.size());
                                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                                } else {
                                    Log.e("TaskAdapter", "Invalid position: " + adapterPosition);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("FirebaseDelete", "Failed to delete task", e);
                                Toast.makeText(context, "Failed to delete task", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(context, "You do not have permission to delete this task", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView taskTextView;
        public CheckBox checkBox;
        public ImageButton editButton;
        public ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTextView = itemView.findViewById(R.id.taskTextView);
            checkBox = itemView.findViewById(R.id.todoCheckBox);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    private void showEditDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Task");

        final EditText input = new EditText(context);
        input.setText(task.getTaskText());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newText = input.getText().toString();
            if (!newText.equals(task.getTaskText())) {
                task.setTaskText(newText);

                DatabaseReference tasksRef = FirebaseDatabase.getInstance().getReference()
                        .child("tasks").child(task.getUserId()).child(task.getId());
                tasksRef.child("taskText").setValue(newText)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("FirebaseUpdate", "Task updated successfully");
                            Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseUpdate", "Failed to update task", e);
                            Toast.makeText(context, "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(context, "No changes made", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
