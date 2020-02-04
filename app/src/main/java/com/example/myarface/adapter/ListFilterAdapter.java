package com.example.myarface.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myarface.R;
import com.example.myarface.model.Filter;

import java.util.ArrayList;

public class ListFilterAdapter extends RecyclerView.Adapter<ListFilterAdapter.ListViewHolder> {
    private ArrayList<Filter> filters;

    public ListFilterAdapter(ArrayList<Filter> filters) {
        this.filters = filters;
    }

    @NonNull
    @Override
    public ListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ListViewHolder holder, int position) {
        Filter filter = filters.get(position);

        holder.tvName.setText(filter.getName());
        holder.imgPhoto.setImageResource(R.color.colorAccent);
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    public class ListViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        ImageView imgPhoto;

        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.img_item_photo);
            tvName = itemView.findViewById(R.id.tv_item_name);
        }
    }
}
