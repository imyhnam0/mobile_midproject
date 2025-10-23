package com.example.imageviewdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    private final List<Post> items = new ArrayList<>();
    private final OnItemClickListener clickListener;

    public ImageAdapter(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<Post> posts) {
        items.clear();
        if (posts != null) {
            items.addAll(posts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Post post = items.get(position);
        holder.bind(post, clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView titleView;
        private final TextView contentView;
        private final TextView authorView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            titleView = itemView.findViewById(R.id.textTitle);
            contentView = itemView.findViewById(R.id.textContent);
            authorView = itemView.findViewById(R.id.textAuthor);
        }

        void bind(Post post, OnItemClickListener listener) {
            imageView.setImageBitmap(post.getBitmap());
            titleView.setText(post.getTitle());
            contentView.setText(post.getContent());

            String author = post.getAuthor();
            if (author == null || author.isEmpty()) {
                authorView.setVisibility(View.GONE);
            } else {
                authorView.setVisibility(View.VISIBLE);
                authorView.setText(author);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(post);
                }
            });
        }
    }
}
