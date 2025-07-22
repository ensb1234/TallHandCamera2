package com.scl.tallhandcamera2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author 作者：涉川良 联系方式：QQ470707134
 * @date 2025/5/14
 * @Description
 */
public class SortableRadioAdapter extends RecyclerView.Adapter<SortableRadioAdapter.ViewHolder> {
    private List<String> items;
    private int selectedPosition = -1;

    public SortableRadioAdapter(List<String> items) {
        this.items = items;
    }

    public SortableRadioAdapter(CharSequence[] entries){
        this.items = new ArrayList<>();

        for (CharSequence cs : entries) {
            this.items.add(cs.toString());
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public List<String> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_radio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(items.get(position));
        holder.radioButton.setChecked(position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void swapItems(int fromPosition, int toPosition) {
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void selectItem(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textView;
        RadioButton radioButton;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text);
            radioButton = itemView.findViewById(R.id.radio_button);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            selectItem(getAdapterPosition());
        }
    }
}
