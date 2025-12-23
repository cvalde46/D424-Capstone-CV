package com.example.vacationapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vacationapp.R;
import com.example.vacationapp.data.entity.VacationEntity;

import java.util.ArrayList;
import java.util.List;

public class VacationAdapter extends RecyclerView.Adapter<VacationAdapter.ViewHolder> {

    public interface OnVacationClickListener {
        void onVacationClick(VacationEntity vacation); // tap row -> edit vacation
    }

    public interface OnVacationShareClickListener {
        void onVacationShareClick(VacationEntity vacation);
    }

    public interface OnVacationExcursionsClickListener {
        void onVacationExcursionsClick(VacationEntity vacation); // button -> excursion list
    }

    private final OnVacationClickListener clickListener;
    private final OnVacationShareClickListener shareListener;
    private final OnVacationExcursionsClickListener excursionsListener;

    private final List<VacationEntity> vacations = new ArrayList<>();

    public VacationAdapter(OnVacationClickListener clickListener,
                           OnVacationShareClickListener shareListener,
                           OnVacationExcursionsClickListener excursionsListener) {
        this.clickListener = clickListener;
        this.shareListener = shareListener;
        this.excursionsListener = excursionsListener;
    }

    public void setVacations(List<VacationEntity> newVacations) {
        vacations.clear();
        if (newVacations != null) vacations.addAll(newVacations);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vacation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VacationEntity vacation = vacations.get(position);

        holder.titleText.setText(vacation.getTitle());

        holder.itemView.setOnClickListener(v -> clickListener.onVacationClick(vacation));
        holder.shareButton.setOnClickListener(v -> shareListener.onVacationShareClick(vacation));
        holder.excursionsButton.setOnClickListener(v -> excursionsListener.onVacationExcursionsClick(vacation));
    }

    @Override
    public int getItemCount() {
        return vacations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final Button shareButton;
        final Button excursionsButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.vacationTitleText);
            shareButton = itemView.findViewById(R.id.shareVacationButton);
            excursionsButton = itemView.findViewById(R.id.excursionsButton);
        }
    }
}
