package com.example.vacationapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vacationapp.R;
import com.example.vacationapp.data.entity.ExcursionEntity;

import java.util.ArrayList;
import java.util.List;

public class ExcursionAdapter extends RecyclerView.Adapter<ExcursionAdapter.ViewHolder> {

    public interface OnExcursionClickListener {
        void onExcursionClick(ExcursionEntity excursion);
    }

    private final OnExcursionClickListener clickListener;
    private final List<ExcursionEntity> excursions = new ArrayList<>();

    public ExcursionAdapter(OnExcursionClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setExcursions(List<ExcursionEntity> list) {
        excursions.clear();
        if (list != null) excursions.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_excursion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExcursionEntity e = excursions.get(position);

        holder.title.setText(safe(e.getTitle()));
        holder.date.setText(safe(e.getDate())); // stored as YYYY-MM-DD for now

        holder.itemView.setOnClickListener(v -> clickListener.onExcursionClick(e));
        holder.editButton.setOnClickListener(v -> clickListener.onExcursionClick(e));
    }

    @Override
    public int getItemCount() {
        return excursions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title; //title and date for B4 is here
        final TextView date;
        final Button editButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.excursionTitleText);
            date = itemView.findViewById(R.id.excursionDateText);
            editButton = itemView.findViewById(R.id.shareExcursionButton);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
