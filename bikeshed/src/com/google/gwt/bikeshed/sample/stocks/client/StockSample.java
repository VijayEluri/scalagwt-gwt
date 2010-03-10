/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.bikeshed.sample.stocks.client;

import com.google.gwt.bikeshed.cells.client.FieldUpdater;
import com.google.gwt.bikeshed.list.client.PagingTableListView;
import com.google.gwt.bikeshed.list.client.TextHeader;
import com.google.gwt.bikeshed.list.shared.AsyncListModel;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.list.shared.Range;
import com.google.gwt.bikeshed.list.shared.AsyncListModel.DataSource;
import com.google.gwt.bikeshed.sample.stocks.client.TransactionTreeViewModel.SectorListModel;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuote;
import com.google.gwt.bikeshed.sample.stocks.shared.StockQuoteList;
import com.google.gwt.bikeshed.sample.stocks.shared.StockRequest;
import com.google.gwt.bikeshed.sample.stocks.shared.StockResponse;
import com.google.gwt.bikeshed.sample.stocks.shared.Transaction;
import com.google.gwt.bikeshed.tree.client.SideBySideTreeView;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockSample implements EntryPoint, Updater {

  /**
   * The delay between updates in milliseconds.
   */
  private static final int UPDATE_DELAY = 5000;
  
  static String getFormattedPrice(int price) {
    return NumberFormat.getCurrencyFormat("USD").format(price / 100.0);
  }

  /**
   * The popup used to purchase stock.
   */
  private BuySellPopup buySellPopup = new BuySellPopup();

  private final Label cashLabel = new Label();

  /**
   * The {@link StockService} used to retrieve data.
   */
  private final StockServiceAsync dataService = GWT.create(StockService.class);

  private AsyncListModel<StockQuote> favoritesListModel;
  
  private PagingTableListView<StockQuote> favoritesTable;

  private final Label netWorthLabel = new Label();
  
  private StockQueryWidget queryWidget;

  private AsyncListModel<StockQuote> searchListModel;
  
  private Map<String, ListListModel<Transaction>> transactionListListModelsByTicker =
    new HashMap<String, ListListModel<Transaction>>();
  
  private ListListModel<Transaction> transactionListModel;
  
  private List<Transaction> transactions;

  private PagingTableListView<Transaction> transactionTable;

  private SideBySideTreeView transactionTree;

  /**
   * The timer used to update the stock quotes.
   */
  private Timer updateTimer = new Timer() {
    @Override
    public void run() {
      update();
    }
  };

  private TransactionTreeViewModel treeModel;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {

    // Create the list models
    searchListModel = new AsyncListModel<StockQuote>(
        new DataSource<StockQuote>() {
          public void requestData(AsyncListModel<StockQuote> listModel) {
            update();
          }
        });

    favoritesListModel = new AsyncListModel<StockQuote>(
        new DataSource<StockQuote>() {
          public void requestData(AsyncListModel<StockQuote> listModel) {
            update();
          }
        });
    
    transactionListModel = new ListListModel<Transaction>();
    transactions = transactionListModel.getList();

    // Create the favorites table.
    favoritesTable = new PagingTableListView<StockQuote>(favoritesListModel, 10);
    favoritesTable.addColumn(Columns.tickerColumn, new TextHeader("ticker"));
    favoritesTable.addColumn(Columns.priceColumn, new TextHeader("price"));
    favoritesTable.addColumn(Columns.sharesColumn, new TextHeader("shares"));
    favoritesTable.addColumn(Columns.dollarsColumn, new TextHeader("value"));
    favoritesTable.addColumn(Columns.buyColumn);
    favoritesTable.addColumn(Columns.sellColumn);
    
    // Create the transactions table.
    transactionTable = new PagingTableListView<Transaction>(transactionListModel, 10);
    transactionTable.addColumn(Columns.transactionColumn);
    transactionTable.addColumn(Columns.subtotalColumn);
    
    treeModel = new TransactionTreeViewModel(this,
        favoritesListModel, transactionListListModelsByTicker);
    transactionTree = new SideBySideTreeView(treeModel, null, 200, 200);

    Columns.favoriteColumn.setFieldUpdater(new FieldUpdater<StockQuote, Boolean>() {
      public void update(StockQuote object, Boolean value) {
        setFavorite(object.getTicker(), value);
      }
    });

    Columns.buyColumn.setFieldUpdater(new FieldUpdater<StockQuote, String>() {
      public void update(StockQuote object, String value) {
        buySellPopup.setStockQuote(object, true);
        buySellPopup.center();
      }
    });

    Columns.sellColumn.setFieldUpdater(new FieldUpdater<StockQuote, String>() {
      public void update(StockQuote object, String value) {
        buySellPopup.setStockQuote(object, false);
        buySellPopup.center();
      }
    });

    buySellPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
      public void onClose(CloseEvent<PopupPanel> event) {
        Transaction t = buySellPopup.getTransaction();
        if (t != null) {
          dataService.transact(t, new AsyncCallback<Transaction>() {
            public void onFailure(Throwable caught) {
              Window.alert("Error: " + caught.getMessage());
            }

            public void onSuccess(Transaction result) {
              recordTransaction(result);
              update();
            }

            /**
             * Update transactions (list of all transactions),
             * transactionTickers (set of all tickers involved in
             * transactions), and transactionsByTicker (map from
             * ticker to lists of transactions for that ticker).
             */
            private void recordTransaction(Transaction result) {
              transactions.add(0, result);
              String ticker = result.getTicker();
              
              // Update the next level of the transaction tree
              // for the given ticker
              ListListModel<Transaction> t =
                transactionListListModelsByTicker.get(ticker);
              if (t == null) {
                t = new ListListModel<Transaction>();
                transactionListListModelsByTicker.put(ticker, t);
              }
              t.getList().add(result);
            }
          });
       }
      }
    });

    // Add components to the page.
    
    Widget headerWidget = new HTML("<b>Stock Game</b>");
    
    HorizontalPanel cashPanel = new HorizontalPanel();
    cashPanel.add(new HTML("<b>Available cash:</b>"));
    cashPanel.add(cashLabel);
    HorizontalPanel netWorthPanel = new HorizontalPanel();
    netWorthPanel.add(new HTML("<b>Net worth:</b>"));
    netWorthPanel.add(netWorthLabel);
    VerticalPanel moneyPanel = new VerticalPanel();
    moneyPanel.add(cashPanel);
    moneyPanel.add(netWorthPanel);
    
    DockLayoutPanel westPanel = new DockLayoutPanel(Unit.PCT);
    westPanel.addNorth(moneyPanel, 25.0);
    westPanel.add(new HTML("<table>" +
        "<tr><td>Dan Rice</td><td>$10000</td></tr>" +
        "<tr><td>Joel Webber</td><td>$10000</td></tr>" +
        "<tr><td>John Labanca</td><td>$10000</td></tr>" +
        "</table>"));
    
    DockLayoutPanel layoutPanel = new DockLayoutPanel(Unit.EM);
    layoutPanel.addNorth(headerWidget, 4.0);
    layoutPanel.addWest(westPanel, 15.0);
    layoutPanel.addNorth(transactionTree, 18.0);
    
    DockLayoutPanel innerLayoutPanel = new DockLayoutPanel(Unit.PCT);
    this.queryWidget = new StockQueryWidget(searchListModel, this);
    innerLayoutPanel.addWest(queryWidget, 60.0);
    
    DockLayoutPanel favoritesLayoutPanel = new DockLayoutPanel(Unit.EM);
    favoritesLayoutPanel.addNorth(new Label("Portfolio / Favorites"), 2.0);
    favoritesLayoutPanel.add(new ScrollPanel(favoritesTable));
    innerLayoutPanel.add(favoritesLayoutPanel);
    layoutPanel.add(innerLayoutPanel);

    RootLayoutPanel.get().add(layoutPanel);

    update();
  }

  /**
   * Set or unset a ticker symbol as a 'favorite'.
   *
   * @param ticker the ticker symbol
   * @param favorite if true, make the stock a favorite
   */
  public void setFavorite(String ticker, boolean favorite) {
    if (favorite) {
      dataService.addFavorite(ticker, favoritesListModel.getRanges()[0],
          new AsyncCallback<StockResponse>() {
        public void onFailure(Throwable caught) {
          Window.alert("Error adding favorite");
        }

        public void onSuccess(StockResponse response) {
          updateFavorites(response);
        }
      });
    } else {
      dataService.removeFavorite(ticker, favoritesListModel.getRanges()[0],
          new AsyncCallback<StockResponse>() {
        public void onFailure(Throwable caught) {
          Window.alert("Error removing favorite");
        }

        public void onSuccess(StockResponse response) {
          updateFavorites(response);
        }
      });
    }
  }
  
  /**
   * Request data from the server using the last query string.
   */
  public void update() {
    if (queryWidget == null) {
      return;
    }
    
    updateTimer.cancel();
  
    Range[] searchRanges = searchListModel.getRanges();
    Range[] favoritesRanges = favoritesListModel.getRanges();
    SectorListModel sectorListModel = treeModel.getSectorListModel();
    Range[] sectorRanges = sectorListModel == null ? null : sectorListModel.getRanges();
  
    if (searchRanges == null || searchRanges.length == 0
        || favoritesRanges == null || favoritesRanges.length == 0) {
      return;
    }
  
    String searchQuery = queryWidget.getSearchQuery();
    StockRequest request = new StockRequest(searchQuery,
        sectorListModel != null ? sectorListModel.getSector() : null,
        searchRanges[0],
        favoritesRanges[0],
        sectorRanges != null && sectorRanges.length > 0 ? sectorRanges[0] : null);
    dataService.getStockQuotes(request, new AsyncCallback<StockResponse>() {
      public void onFailure(Throwable caught) {
        String message = caught.getMessage();
        if (message.contains("Not logged in")) {
          // Force the user to login.
          Window.Location.reload();
        } else {
          Window.alert("ERROR: " + caught.getMessage());
          updateTimer.schedule(UPDATE_DELAY);
        }
      }
  
      public void onSuccess(StockResponse result) {
        processStockResponse(result);
      }
    });
  }

  /**
   * Process the {@link StockResponse} from the server.
   *
   * @param response the stock response
   */
  private void processStockResponse(StockResponse response) {
    // Update the search list.
    StockQuoteList searchResults = response.getSearchResults();
    searchListModel.updateDataSize(response.getNumSearchResults(), true);
    searchListModel.updateViewData(searchResults.getStartIndex(),
        searchResults.size(), searchResults);

    // Update the favorites list.
    updateFavorites(response);
    updateSector(response);

    // Update available cash.
    int cash = response.getCash();
    int netWorth = response.getNetWorth();
    cashLabel.setText(getFormattedPrice(cash));
    netWorthLabel.setText(getFormattedPrice(netWorth));
    buySellPopup.setAvailableCash(cash);

    // Restart the update timer.
    updateTimer.schedule(UPDATE_DELAY);
  }

  private void updateFavorites(StockResponse response) {
    // Update the favorites list.
    StockQuoteList favorites = response.getFavorites();
    favoritesListModel.updateDataSize(response.getNumFavorites(), true);
    favoritesListModel.updateViewData(favorites.getStartIndex(),
        favorites.size(), favorites);
  }

  private void updateSector(StockResponse response) {
    // Update the sector list.
    StockQuoteList sectorList = response.getSector();
    if (sectorList != null) {
      SectorListModel sectorListModel = treeModel.getSectorListModel(); 
      sectorListModel.updateDataSize(response.getNumSector(), true);
      sectorListModel.updateViewData(sectorList.getStartIndex(),
          sectorList.size(), sectorList);
    }
  }
}