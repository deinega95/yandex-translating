package dise.yandextranslate.adapters;
import android.view.LayoutInflater;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import dise.yandextranslate.R;

//адаптер для отображения вариантов перевода, полученных от словаря
public class DictionaryAdapter extends BaseAdapter{

    Context cnt;
    LayoutInflater lInflater;
    ArrayList<ArrayList<String>> data;

    public DictionaryAdapter(Context context, ArrayList<ArrayList<String>> data) {
        cnt = context;
        this.data = data;
        lInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.item_dictionary, parent, false);
        }
        try {
            ArrayList<String> elem = getSubject(pos);
            ((TextView) view.findViewById(R.id.tv_number)).setText(elem.get(0));
            ((TextView) view.findViewById(R.id.tv_variable)).setText(elem.get(1));
            ((TextView) view.findViewById(R.id.tv_translate)).setText(elem.get(2));
        } catch (Exception e){}


        return view;
    }

    public ArrayList<String> getSubject(int position) {
        return ((ArrayList<String>)getItem(position));
    }

}
