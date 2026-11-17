package com.example.albertup.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.albertup.R;
import com.example.albertup.data.WorkoutSet;

import java.util.Objects;

/** Day-grouped list of logged sets with edit and delete actions. */
public final class HistoryAdapter extends ListAdapter<HistoryItem, RecyclerView.ViewHolder> {

    public interface Listener {
        void onEdit(@NonNull WorkoutSet set);

        void onDelete(@NonNull WorkoutSet set);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;

    private static final DiffUtil.ItemCallback<HistoryItem> DIFF =
            new DiffUtil.ItemCallback<HistoryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull HistoryItem oldItem,
                                               @NonNull HistoryItem newItem) {
                    return oldItem.isSameItemAs(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull HistoryItem oldItem,
                                                  @NonNull HistoryItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    private final Listener listener;

    public HistoryAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isHeader() ? TYPE_HEADER : TYPE_ENTRY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_history_header, parent, false));
        }
        return new EntryHolder(inflater.inflate(R.layout.item_history_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryItem item = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(item);
        } else {
            ((EntryHolder) holder).bind(Objects.requireNonNull(item.getSet()), listener);
        }
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView tvDay;
        private final TextView tvDaySummary;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvDaySummary = itemView.findViewById(R.id.tvDaySummary);
        }

        void bind(@NonNull HistoryItem item) {
            tvDay.setText(item.getTitle());
            tvDaySummary.setText(item.getSummary());
        }
    }

    static final class EntryHolder extends RecyclerView.ViewHolder {
        private final ImageView ivExercise;
        private final TextView tvTitle;
        private final TextView tvTime;
        private final Button btnEdit;
        private final Button btnDelete;

        EntryHolder(@NonNull View itemView) {
            super(itemView);
            ivExercise = itemView.findViewById(R.id.ivExercise);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(@NonNull WorkoutSet set, @NonNull Listener listener) {
            Context context = itemView.getContext();
            String count = Formats.exerciseCount(context, set.getExercise(), set.getReps());
            String time = Formats.time(context, set.getPerformedAt());
            ivExercise.setImageResource(set.getExercise().getIconRes());
            tvTitle.setText(count);
            tvTime.setText(time);

            itemView.setOnClickListener(v -> listener.onEdit(set));
            ViewCompat.replaceAccessibilityAction(itemView, AccessibilityActionCompat.ACTION_CLICK,
                    context.getString(R.string.action_edit), null);
            btnEdit.setContentDescription(
                    context.getString(R.string.history_edit_desc, count, time));
            btnEdit.setOnClickListener(v -> listener.onEdit(set));
            btnDelete.setContentDescription(
                    context.getString(R.string.history_delete_desc, count, time));
            btnDelete.setOnClickListener(v -> listener.onDelete(set));
        }
    }
}
