package de.shellfire.vpn.android;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import de.shellfire.vpn.android.openvpn.VpnStatus;
import de.shellfire.vpn.android.webservice.ShellfireWebService;

public class SelectVpnActivity extends Activity {

	private static ShellfireWebService webService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_vpn);

		// check if we are being restored, e.g. for orientation changes
		webService = ShellfireWebService.getInstance();
		
		
		// make sure to only uncheck if user has unchecked in the past, thereby
		// having the checkbox
		// enabled the first time, but if the user unchecks it, it wont be
		// enabled again everytime
		// he or she logs in again
		if (VpnPreferences.containsRememberVpnSelection(this)) {
			Boolean remember = VpnPreferences.getRememberVpnSelection(this);

			CheckBox box = (CheckBox) findViewById(R.id.checkBoxRememberVpnSelection);
			box.setChecked(remember);
		}

		new RetrieveVpnListTask().execute();
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return webService;
	}
	
	public void onclickCheckBoxRememberVpnSelection(View v) {
		CheckBox box = (CheckBox) v;

		VpnPreferences.setRememberVpnSelection(this, box.isChecked());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_vpn, menu);
		return true;
	}

	private void returnVpnToMainActivity(Vpn vpn) {
		Bundle conData = new Bundle();
		conData.putInt("selectedVpnId", vpn.getVpnId());

		Intent intent = new Intent();
		intent.putExtras(conData);
		setResult(RESULT_OK, intent);

		finish();
	}

	private class VpnListAdapter extends ArrayAdapter<Vpn> {

		HashMap<Vpn, Integer> mIdMap = new HashMap<Vpn, Integer>();
		private List<Vpn> mObjects;

		public VpnListAdapter(Context context, int textViewResourceId, List<Vpn> objects) {
			super(context, textViewResourceId, objects);
			for (int i = 0; i < objects.size(); ++i) {
				mIdMap.put(objects.get(i), i);
				mObjects = objects;
			}
		}

		@Override
		public long getItemId(int position) {
			Vpn item = getItem(position);
			return mIdMap.get(item);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Vpn vpn = mObjects.get(position);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.vpn_list_item, parent, false);

			// assign vpn name
			TextView nameView = (TextView) rowView.findViewById(R.id.vpnName);
			nameView.setText(vpn.getName().trim());

			
			// assign vpnAccountType (Free, Premium, PremiumPlus)  + stars
			TextView vpnAccountTypeView = (TextView) rowView.findViewById(R.id.vpnAccountType);
			vpnAccountTypeView.setText(Util.getServerTypeResId(vpn.getAccountType()));
						
			int numStars;
			if (vpn.getAccountType() == ServerType.Free)
				numStars = 1;
			else if (vpn.getAccountType() == ServerType.Premium)
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
				starView.setVisibility(android.view.View.INVISIBLE);
			}

			return rowView;
		}

	}

	class RetrieveVpnListTask extends AsyncTask<Void, Void, LinkedList<Vpn>> {

		protected LinkedList<Vpn> doInBackground(Void... zoid) {
			synchronized (webService) {

				try {
					final LinkedList<Vpn> vpnList = webService.getAllVpnDetails(false);

					return vpnList;
				} catch (Exception e) {
					VpnStatus.logError(e);
					e.printStackTrace();
				}
			}

			return null;
		}

		protected void onPostExecute(final LinkedList<Vpn> vpnList) {

			// 3 shortcuts possible:
			// - no vpn yet available -> automatically create a free one, select
			// it and forward to MainActivity
			// - exactly 1 vpn available, automatically select it and forwad to
			// MainActivity
			// - several vpns available, but 1 had been chosen in the past and
			// stored to be remembered
			boolean createList = true;
			if (vpnList == null || vpnList.size() == 0) {
				new CreateFreeVpnTask().execute();
				createList = false;

			} else if (vpnList.size() == 1) {
				returnVpnToMainActivity(vpnList.get(0));
				createList = false;

			} else if (VpnPreferences.getRememberVpnSelection(getBaseContext())) {

				int rememberedVpnid = VpnPreferences.getRememberedVpnSelection(getBaseContext());

				// try to find this id in the list
				for (Vpn vpn : vpnList) {
					if (vpn.getVpnId() == rememberedVpnid) {
						createList = false;
						returnVpnToMainActivity(vpn);
					}
				}

				// if we did not find this vpn, remove it from the preferences
				VpnPreferences.setRememberedVpnSelection(getBaseContext(), null);

				// and show the list to the user
				createList = true;
			}

			if (createList) {
				// not shortcut possible - user interaction needed
				final ListView listview = (ListView) findViewById(R.id.listViewVpnSelection);
				final VpnListAdapter adapter = new VpnListAdapter(getBaseContext(), R.layout.vpn_list_item, vpnList);
				listview.setAdapter(adapter);

				listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
						final Vpn item = (Vpn) parent.getItemAtPosition(position);

						// store the selected vpn if checkbox to store checked

						CheckBox box = (CheckBox) findViewById(R.id.checkBoxRememberVpnSelection);

						VpnPreferences.setRememberVpnSelection(getBaseContext(), box.isChecked());

						if (box.isChecked()) {
							VpnPreferences.setRememberedVpnSelection(getBaseContext(), item.getVpnId());
						}

						returnVpnToMainActivity(item);

					}

				});
			}

		}

		class CreateFreeVpnTask extends AsyncTask<Void, Void, Vpn> {

			protected Vpn doInBackground(Void... zoid) {
				synchronized (webService) {

					try {
						final Vpn vpn = webService.createNewFreeVpn();

						return vpn;
					} catch (Exception e) {
						VpnStatus.logError(e);
						e.printStackTrace();
					}
				}

				return null;
			}

			protected void onPostExecute(final Vpn vpn) {
				// automatically handle the freshly created vpn over to the
				// MainActivity
				if (vpn != null)
					returnVpnToMainActivity(vpn);
			}

		}

	}

}
