package com.example.foodorderingpos;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    final String PREF="food_pos_data";
    final ArrayList<Item> menu=new ArrayList<>();
    final LinkedHashMap<String,Integer> cart=new LinkedHashMap<>();
    final ArrayList<Order> orders=new ArrayList<>();
    LinearLayout page, content;
    String category="All", paper="80mm";
    EditText tableInput;
    TextView totalView;
    BluetoothDevice printer;

    static class Item {
        String name, category; double price;
        Item(String n,String c,double p){name=n;category=c;price=p;}
    }
    static class Order {
        int number; String table,status,date,lines; double total;
        Order(int n,String t,String s,String d,String l,double v){number=n;table=t;status=s;date=d;lines=l;total=v;}
    }

    @Override public void onCreate(Bundle b){super.onCreate(b); load(); if(menu.isEmpty()) seed(); showHome();}
    void seed(){
        add("Chicken Rice","Rice",7.50); add("Fried Rice","Rice",7.00); add("Nasi Lemak","Rice",6.00);
        add("Fried Kuey Teow","Noodles",7.00); add("Mee Goreng","Noodles",7.00); add("Curry Mee","Noodles",8.00);
        add("Teh Ais","Drinks",2.50); add("Kopi O","Drinks",2.00); add("Milo","Drinks",3.00);
        add("French Fries","Snacks",5.00); add("Fried Chicken","Snacks",6.00);
        save();
    }
    void add(String n,String c,double p){menu.add(new Item(n,c,p));}
    void load(){
        String raw=getSharedPreferences(PREF,0).getString("menu","");
        try { JSONArray a=new JSONArray(raw); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);add(o.getString("n"),o.getString("c"),o.getDouble("p"));}}catch(Exception e){}
        String saved=getSharedPreferences(PREF,0).getString("orders","");
        try {JSONArray a=new JSONArray(saved);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);orders.add(new Order(o.getInt("n"),o.getString("t"),o.getString("s"),o.getString("d"),o.getString("l"),o.getDouble("v")));}}catch(Exception e){}
        paper=getSharedPreferences(PREF,0).getString("paper","80mm");
    }
    void save(){
        try{
            JSONArray a=new JSONArray();for(Item x:menu){JSONObject o=new JSONObject();o.put("n",x.name);o.put("c",x.category);o.put("p",x.price);a.put(o);}
            JSONArray b=new JSONArray();for(Order x:orders){JSONObject o=new JSONObject();o.put("n",x.number);o.put("t",x.table);o.put("s",x.status);o.put("d",x.date);o.put("l",x.lines);o.put("v",x.total);b.put(o);}
            getSharedPreferences(PREF,0).edit().putString("menu",a.toString()).putString("orders",b.toString()).putString("paper",paper).apply();
        }catch(Exception e){}
    }
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
    TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xff202522);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(dp(8),dp(8),dp(8),dp(8));return t;}
    Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
    void base(String title){
        page=new LinearLayout(this);page.setOrientation(1);page.setBackgroundColor(0xfff4f7f5);
        TextView h=text(title,23,true);h.setTextColor(-1);h.setBackgroundColor(0xff126b49);h.setPadding(dp(18),dp(18),dp(12),dp(18));page.addView(h);
        ScrollView sc=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(12),dp(8),dp(12),dp(18));sc.addView(content);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(page);
    }
    void nav(){LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.addView(button("New Order",()->showOrder()),new LinearLayout.LayoutParams(0,dp(56),1));row.addView(button("Orders",()->showOrders()),new LinearLayout.LayoutParams(0,dp(56),1));row.addView(button("Menu",()->showMenu()),new LinearLayout.LayoutParams(0,dp(56),1));row.addView(button("Settings",()->showSettings()),new LinearLayout.LayoutParams(0,dp(56),1));page.addView(row);}
    void showHome(){base("Food Ordering POS");content.addView(text("Welcome",24,true));content.addView(text("Waiter ordering • Offline • No payment collection",15,false));
        content.addView(button("＋  CREATE NEW ORDER",()->showOrder()));content.addView(button("ORDER HISTORY",()->showOrders()));content.addView(button("MENU MANAGEMENT",()->showMenu()));
        double sales=0;for(Order o:orders)if(o.status.equals("Completed"))sales+=o.total;
        content.addView(text("Completed sales today: RM "+String.format(Locale.US,"%.2f",sales),18,true));
        content.addView(button("PRINTER & SETTINGS",()->showSettings()));nav();}
    void showOrder(){
        cart.clear();base("New Order");
        content.addView(text("Table number",16,true));tableInput=new EditText(this);tableInput.setSingleLine(true);tableInput.setHint("e.g. Table 5");content.addView(tableInput);
        LinearLayout cats=new LinearLayout(this);cats.setOrientation(0);
        ArrayList<String> cs=new ArrayList<>(Arrays.asList("All","Rice","Noodles","Drinks","Snacks"));
        for(String c:cs)cats.addView(button(c,()->{category=c;showMenuItems();}),new LinearLayout.LayoutParams(0,dp(48),1));
        content.addView(cats);
        LinearLayout list=new LinearLayout(this);list.setOrientation(1);content.addView(list); 
        totalView=text("Total: RM 0.00",20,true);content.addView(totalView);
        content.addView(button("SAVE ORDER & PRINT KITCHEN TICKET",()->saveOrder()));
        content.addView(button("Back",()->showHome()));
        menuItemsInto(list);
    }
    void showMenuItems(){
        // Rebuild order screen while retaining cart and table.
        String t=tableInput==null?"":tableInput.getText().toString();
        showOrderKeep(t);
    }
    void showOrderKeep(String t){
        base("New Order");content.addView(text("Table: "+(t.isEmpty()?"Not entered":t),16,true));
        LinearLayout cats=new LinearLayout(this);for(String c:new String[]{"All","Rice","Noodles","Drinks","Snacks"})cats.addView(button(c,()->{category=c;showOrderKeep(t);}),new LinearLayout.LayoutParams(0,dp(44),1));content.addView(cats);
        LinearLayout list=new LinearLayout(this);content.addView(list);menuItemsInto(list);
        totalView=text("Total: RM "+money(cartTotal()),20,true);content.addView(totalView);
        EditText note=new EditText(this);note.setHint("Special instructions (optional)");note.setTag("note");content.addView(note);
        content.addView(button("SAVE ORDER & PRINT KITCHEN TICKET",()->saveOrderWith(t,note.getText().toString())));
        content.addView(button("Back",()->showHome()));
    }
    void menuItemsInto(LinearLayout list){
        for(Item x:menu){if(!category.equals("All")&&!x.category.equals(category))continue;
            LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=text(x.name+"\n"+x.category+" • RM "+money(x.price),16,true);row.addView(label,new LinearLayout.LayoutParams(0,dp(64),1));
            row.addView(button("−",()->{int q=cart.containsKey(x.name)?cart.get(x.name):0;if(q<=1)cart.remove(x.name);else cart.put(x.name,q-1);showOrderKeep("");}));
            row.addView(text(""+(cart.containsKey(x.name)?cart.get(x.name):0),17,true));
            row.addView(button("+",()->{cart.put(x.name,(cart.containsKey(x.name)?cart.get(x.name):0)+1);showOrderKeep("");}));
            list.addView(row);View line=new View(this);line.setBackgroundColor(0xffdce5df);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        }
    }
    double cartTotal(){double v=0;for(Item x:menu)if(cart.containsKey(x.name))v+=x.price*cart.get(x.name);return v;}
    String money(double v){return String.format(Locale.US,"%.2f",v);}
    void saveOrder(){saveOrderWith(tableInput==null?"":tableInput.getText().toString(),"");}
    void saveOrderWith(String table,String note){
        if(cart.isEmpty()){toast("Please add at least one item.");return;}
        int n=orders.isEmpty()?1:orders.get(orders.size()-1).number+1;
        StringBuilder lines=new StringBuilder();for(Item x:menu)if(cart.containsKey(x.name))lines.append(cart.get(x.name)).append(" x ").append(x.name).append("  RM ").append(money(x.price*cart.get(x.name))).append("\n");
        if(!note.trim().isEmpty())lines.append("Note: ").append(note).append("\n");
        String date=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());
        Order o=new Order(n,table.isEmpty()?"-":table,"Pending",date,lines.toString(),cartTotal());orders.add(o);save();printOrder(o);toast("Order #"+n+" saved.");showHome();
    }
    void showOrders(){
        base("Order History");
        if(orders.isEmpty())content.addView(text("No orders yet.",17,false));
        for(Order o:orders){content.addView(text("ORDER #"+o.number+"  •  "+o.status,18,true));content.addView(text("Table: "+o.table+"   "+o.date+"\n"+o.lines+"Total RM "+money(o.total),15,false));
            LinearLayout r=new LinearLayout(this);r.addView(button("Preparing",()->{o.status="Preparing";save();showOrders();}),new LinearLayout.LayoutParams(0,dp(48),1));
            r.addView(button("Completed",()->{o.status="Completed";save();showOrders();}),new LinearLayout.LayoutParams(0,dp(48),1));
            r.addView(button("Reprint",()->printOrder(o)),new LinearLayout.LayoutParams(0,dp(48),1));content.addView(r);
            View v=new View(this);v.setBackgroundColor(0xffccd8d0);content.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        content.addView(button("Back",()->showHome()));nav();
    }
    void showMenu(){
        base("Menu Management");content.addView(text("Add food or drink",18,true));
        EditText name=new EditText(this);name.setHint("Item name");content.addView(name);
        EditText cat=new EditText(this);cat.setHint("Category: Rice / Noodles / Drinks / Snacks");content.addView(cat);
        EditText price=new EditText(this);price.setHint("Price (e.g. 7.50)");price.setInputType(8194);content.addView(price);
        content.addView(button("ADD MENU ITEM",()->{try{if(name.getText().toString().trim().isEmpty())throw new Exception();add(name.getText().toString().trim(),cat.getText().toString().trim().isEmpty()?"Other":cat.getText().toString().trim(),Double.parseDouble(price.getText().toString()));save();toast("Menu item added.");showMenu();}catch(Exception e){toast("Enter item name and a valid price.");}}));
        for(Item x:menu){content.addView(text(x.name+" • "+x.category+" • RM "+money(x.price),16,false));content.addView(button("Delete "+x.name,()->{menu.remove(x);save();showMenu();}));}
        content.addView(button("Back",()->showHome()));nav();
    }
    void showSettings(){
        base("Printer & Settings");content.addView(text("Bluetooth thermal printer",18,true));
        content.addView(button("CONNECT PAIRED BLUETOOTH PRINTER",()->choosePrinter()));
        content.addView(text("Paper width",16,true));
        content.addView(button("58mm"+(paper.equals("58mm")?" ✓":""),()->{paper="58mm";save();showSettings();}));
        content.addView(button("80mm"+(paper.equals("80mm")?" ✓":""),()->{paper="80mm";save();showSettings();}));
        content.addView(text("Pair the printer in Android Bluetooth settings first. This app prints to already-paired ESC/POS Bluetooth printers.",14,false));
        content.addView(button("Back",()->showHome()));nav();
    }
    void choosePrinter(){
        try{
            if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},44);return;}
            BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();if(a==null){toast("Bluetooth is not available.");return;}
            Set<BluetoothDevice> ds=a.getBondedDevices();ArrayList<BluetoothDevice> arr=new ArrayList<>(ds);
            if(arr.isEmpty()){toast("No paired printers. Pair your printer in Android settings first.");return;}
            String[] names=new String[arr.size()];for(int i=0;i<arr.size();i++)names[i]=arr.get(i).getName()+" ("+arr.get(i).getAddress()+")";
            new AlertDialog.Builder(this).setTitle("Select printer").setItems(names,(d,w)->{printer=arr.get(w);toast("Selected "+printer.getName());}).setNegativeButton("Cancel",null).show();
        }catch(Exception e){toast("Could not access Bluetooth. Check permissions.");}
    }
    void printOrder(Order o){
        if(printer==null){toast("Select a paired printer in Settings before printing.");return;}
        new Thread(()->{try{
            BluetoothSocket s=printer.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));s.connect();OutputStream out=s.getOutputStream();
            String width=paper.equals("58mm")?"--------------------------------\n":"------------------------------------------------\n";
            String data="\n       FOOD ORDER\n"+width+"Order: #"+o.number+"\nTable: "+o.table+"\n"+o.date+"\n"+width+o.lines+width+"TOTAL: RM "+money(o.total)+"\nStatus: "+o.status+"\n\n\n";
            out.write(new byte[]{0x1b,0x40});out.write(data.getBytes("UTF-8"));out.write(new byte[]{0x1d,0x56,0x41,0x10});out.flush();s.close();
            runOnUiThread(()->toast("Print sent."));
        }catch(Exception e){runOnUiThread(()->toast("Print failed. Check printer is on and paired."));}}).start();
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
