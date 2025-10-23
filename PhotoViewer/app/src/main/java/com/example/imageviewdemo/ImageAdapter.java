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

    public interface Listener {
        void onItemClick(Post post);
        void onToggleFavorite(Post post);
        void onDelete(Post post);
    }

    private final List<Post> items = new ArrayList<>();
    private final Listener listener;

    public ImageAdapter(Listener listener) {
        this.listener = listener;
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
        holder.bind(post, listener);
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
        private final ImageView favoriteIcon;
        private final ImageView deleteIcon;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
            titleView = itemView.findViewById(R.id.textTitle);
            contentView = itemView.findViewById(R.id.textContent);
            authorView = itemView.findViewById(R.id.textAuthor);
            favoriteIcon = itemView.findViewById(R.id.iconFavorite);
            deleteIcon = itemView.findViewById(R.id.iconDelete);
        }

        void bind(Post post, Listener listener) {
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

            if (favoriteIcon != null) {
                favoriteIcon.setImageResource(post.isFavorite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                favoriteIcon.setContentDescription(post.isFavorite() ? "즐겨찾기 해제" : "즐겨찾기 추가");
                favoriteIcon.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onToggleFavorite(post);
                    }
                });
            }

            if (deleteIcon != null) {
                deleteIcon.setVisibility(View.VISIBLE);
                deleteIcon.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDelete(post);
                    }
                });
            }

        }
    }
}
