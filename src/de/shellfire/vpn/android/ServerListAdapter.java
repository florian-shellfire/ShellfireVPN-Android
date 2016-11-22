package de.shellfire.vpn.android;

import java.util.HashMap;
import java.util.List;

import de.shellfire.vpn.android.webservice.ShellfireWebService;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ServerListAdapter extends ArrayAdapter<Server> {

	HashMap<Server, Integer> mIdMap = new HashMap<Server, Integer>();
	private List<Server> mObjects;
	private Context mContext;

	public ServerListAdapter(Context context, int textViewResourceId, List<Server> objects) {
		super(context, textViewResourceId, objects);
		mContext = context;
		for (int i = 0; i < objects.size(); ++i) {
			mIdMap.put(objects.get(i), i);
			mObjects = objects;
		}
	}

	@Override
	public long getItemId(int position) {
		Server item = getItem(position);
		return mIdMap.get(item);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Server server = mObjects.get(position);

		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.server_list_item, parent, false);

		setServerValues(rowView, server);
		return rowView;
	}

	private int getCountryImageResId(Country country, Resources res) {
		String isoCode = CountryMap.get(country);
		
		int resId = res.getIdentifier(isoCode , "drawable", MainActivity.packageName);

		return resId;
	}
	
	private int getCountryNameResId(Country country, Resources res) {
		int resId = res.getIdentifier(country.toString().toLowerCase() , "string", MainActivity.packageName);

		return resId;
	}		
	
	
	void setServerValues(View rowView, Server server) {
		// assign country-image
		//ImageView countryImageView = (ImageView) rowView.findViewById(R.id.country);
		//countryImageView.setImageResource(countryResId);

		// write server id on flag
		int countryImageResId = getCountryImageResId(server.getCountry(), rowView.getResources());
		TextView countryImageView = (TextView) rowView.findViewById(R.id.country);
		
		countryImageView.setBackgroundResource(countryImageResId);
		countryImageView.setText(mContext.getText(R.string.server) + " "+server.getServerId());
		

		// assign name
		TextView nameView = (TextView) rowView.findViewById(R.id.vpnName);
		int countryNameResId = getCountryNameResId(server.getCountry(), rowView.getResources());
		if (countryNameResId != 0)
			nameView.setText(countryNameResId);
		else
			nameView.setText(""+server.getCountry());
		
		// assign server type
		TextView serverTypeView = (TextView) rowView.findViewById(R.id.vpnAccountType);
		serverTypeView.setText(Util.getServerTypeResId(server.getServerType()));

		int numStars;
		if (server.getServerType() == ServerType.Free)
			numStars = 1;
		else if (server.getServerType() == ServerType.Premium)
			numStars = 3;
		else
			numStars = 5;

		for (int i = 5 - numStars; i >= 1; i--) {
			int id;
			if (i == 1)
				id = R.id.star_1;
			else if (i == 2)
				id = R.id.star_2;
			else if (i == 3)
				id = R.id.star_3;
			else if (i == 4)
				id = R.id.star_4;
			else
				id = R.id.star_5;

			ImageView starView = (ImageView) rowView.findViewById(id);
			starView.setVisibility(android.view.View.GONE);
		}

		// assign speed (incl. stars)
		TextView speedTextView = (TextView) rowView.findViewById(R.id.speedTextView);
		speedTextView.setText(server.getServerSpeed().getResId());
		TextView securityTextView = (TextView) rowView.findViewById(R.id.securityTextView);
		securityTextView.setText(server.getSecurity().getResId());

		// assign diff. background color for selected server
		Server selectedServer = ShellfireWebService.getInstance().getSelectedServer();

		if (selectedServer != null && selectedServer.getServerId() == server.getServerId()) {
			rowView.setBackgroundColor(rowView.getResources().getColor(R.color.selected));
		}
		
		// assign loadPercentage
		int load = server.getLoadPercentage();
		ProgressBar loadBar = (ProgressBar) rowView.findViewById(R.id.loadBar);
		loadBar.setProgress(load);
		loadBar.getProgressDrawable().setAlpha(180);
		
		TextView progressText = (TextView) rowView.findViewById(R.id.progressText);
		progressText.setText(load + " %");
	}
}
