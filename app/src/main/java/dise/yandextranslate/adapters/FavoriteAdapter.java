package dise.yandextranslate.adapters;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;


import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Filterable;

import java.util.ArrayList;

import dise.yandextranslate.R;
import dise.yandextranslate.activities.MainActivity;
import dise.yandextranslate.db.ElementHistoryOrFavorite;
import dise.yandextranslate.db.TranslateDatabaseHelper;

//adapter для отображения избранного
public class FavoriteAdapter extends ArrayAdapter<ElementHistoryOrFavorite> implements Filterable {

    private ArrayList<ElementHistoryOrFavorite> allModelItemsArray;
    private ArrayList<ElementHistoryOrFavorite> filteredModelItemsArray;
    private Activity context;
    private ModelFilter filter;
    private LayoutInflater inflater;
    private TranslateDatabaseHelper translateDatabase;
    private SQLiteDatabase db;

    public FavoriteAdapter(Activity context, ArrayList<ElementHistoryOrFavorite> list) {
        super(context, R.layout.item_taskbar, list);
        this.context = context;
        translateDatabase = new TranslateDatabaseHelper(context);
        db = translateDatabase.getWritableDatabase();
        this.allModelItemsArray = new ArrayList<>();
        allModelItemsArray.addAll(list);
        this.filteredModelItemsArray = new ArrayList<>();
        filteredModelItemsArray.addAll(allModelItemsArray);
        inflater = context.getLayoutInflater();
        getFilter();
    }

    @Override
    public Filter getFilter() {
        if (filter == null){
            filter  = new ModelFilter();
        }
        return filter;
    }
    private static class ViewHolder {
        private ImageButton image;
        private TextView historyTextSource;
        private TextView historyTextTranslating;
        private TextView code;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = null;
        ElementHistoryOrFavorite m = filteredModelItemsArray.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {

            view = inflater.inflate(R.layout.item_taskbar, null);
            viewHolder = new ViewHolder();
            final TextView sourceTextTV = (TextView) view.findViewById(R.id.tv_historyTextSource);
            final TextView textTranslatingTV = (TextView) view.findViewById(R.id.tv_historyTextTranslating);
            final TextView codeTV = (TextView) view.findViewById(R.id.tvCodeTranslateHistory);
            final ImageButton favorite = (ImageButton) view.findViewById(R.id.imageButtonFavorite);

            //обработка нажатия добавление в избранное
            favorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = favorite.getTag().toString();
                    if (str.equals("Favorite")) {
                        String text = sourceTextTV.getText().toString();
                        String textTranslating = textTranslatingTV.getText().toString();
                        String code = codeTV.getText().toString();
                        db.delete("favoriteTranslating", "textForTranslating = ? AND translatingText = ? AND codeTranslating = ?",
                                new String[]{text, textTranslating , code});
                        favorite.setTag("NotFavorite");
                        favorite.setImageResource(R.drawable.favorite_false_icon);
                    } else if (str.equals("NotFavorite")) {
                        String text = sourceTextTV.getText().toString();
                        String textTranslating = textTranslatingTV.getText().toString();
                        String code = codeTV.getText().toString();
                        ContentValues record = new ContentValues();
                        record.put("textForTranslating", text.toLowerCase().trim());
                        record.put("translatingText", textTranslating.toLowerCase().trim());
                        record.put("codeTranslating", code);
                        db.insert("favoriteTranslating", null, record);
                        favorite.setTag("Favorite");
                        favorite.setImageResource(R.drawable.favorite_icon);
                    }
                }
            });
            viewHolder.historyTextSource = sourceTextTV;
            viewHolder.historyTextTranslating = textTranslatingTV;
            viewHolder.image = favorite;

            viewHolder.code = codeTV;
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((ViewHolder) view.getTag());
        }
        viewHolder.historyTextSource.setText(m.getText());
        viewHolder.historyTextTranslating.setText(m.getTranslatingText());
        viewHolder.code.setText(m.getCodeTranslating());
        if (isFavorite(m.getText(), m.getCodeTranslating())) {
            viewHolder.image.setImageResource(R.drawable.favorite_icon);
            viewHolder.image.setTag("Favorite");
        } else {
            viewHolder.image.setImageResource(R.drawable.favorite_false_icon);
            viewHolder.image.setTag("NotFavorite");
        }
        return view;
    }

    //проверяем является ли запись избранной
    public boolean isFavorite(String text, String codeTransalting) {
        if (db.isOpen()) {
            Cursor cursor = db.query("favoriteTranslating", new String[]{"_id"},
                    "textForTranslating = ? AND codeTranslating = ?", new String[]{text, codeTransalting}, null, null, null);
            if (cursor.getCount() > 0) {
                return true;
            }
            cursor.close();
        }
        return false;
    }

    //фильтр для отображения введенных данных
    private class ModelFilter extends Filter
    {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            constraint = constraint.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if(constraint.toString().length() > 0)
            {
                ArrayList<ElementHistoryOrFavorite> filteredItems = new ArrayList<>();

                for(int i = 0, l = allModelItemsArray.size(); i < l; i++)
                {
                    String m = allModelItemsArray.get(i).getText();
                    if(m.toLowerCase().contains(constraint))
                        filteredItems.add(allModelItemsArray.get(i));
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            }
            else
            {
                synchronized(this)
                {
                    result.values = allModelItemsArray;
                    result.count = allModelItemsArray.size();
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {

            filteredModelItemsArray = (ArrayList<ElementHistoryOrFavorite>)results.values;
            notifyDataSetChanged();
            clear();
            for(int i = 0, l = filteredModelItemsArray.size(); i < l; i++)
                add(filteredModelItemsArray.get(i));
            notifyDataSetInvalidated();
        }
    }
}