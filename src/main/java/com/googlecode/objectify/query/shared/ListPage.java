/*
 * Brendan Doherty, trading as PropertySimplified ("COMPANY") CONFIDENTIAL
 * Unpublished Copyright (c) 2010 PropertySimplified, All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains the property of
 * COMPANY. The intellectual and technical concepts contained herein are
 * proprietary to COMPANY and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from COMPANY.
 * Access to the source code contained herein is hereby forbidden to anyone
 * except current COMPANY employees, managers or contractors who have executed
 * Confidentiality and Non-disclosure agreements explicitly covering such
 * access.
 * 
 * The copyright notice above does not evidence any actual or intended
 * publication or disclosure of this source code, which includes information
 * that is confidential and/or proprietary, and is a trade secret, of COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC
 * DISPLAY OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN
 * CONSENT OF COMPANY IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE
 * LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION OF THIS SOURCE
 * CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO
 * REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR
 * SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.googlecode.objectify.query.shared;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * A "page" of results returned from an objectify-query generated query object.
 * <p/>
 * If there are more results still available to be returned from the query, 
 * the <code>more</code> operation will return true.  
 * 
 * Use the <code>getCursor</code> operation to retrieve the pointer to the 
 * last location of the query.   
 * 
 * ListPage is GWT-RPC friendly.
 * 
 * @author Brendan Doherty
 *
 * @param <Entity_> 
 */
public class ListPage<Entity_> implements Iterable<Entity_>, RandomAccess, IsSerializable {

	private ArrayList<Entity_> list;
	private String cursor;
	private boolean more;

	ListPage() {
	}

	public ListPage(ArrayList<Entity_> list, String cursor, boolean more) {
		assert list != null;
		this.list = list;
		this.cursor = cursor;
		this.more = more;
	}

	public boolean contains(Entity_ entity) {
		return this.list.contains(entity);		
	}

	public Entity_ get(int index) {
		return this.list.get(index);
	}

	public int indexOf(Entity_ entity) {
		return this.list.indexOf(entity);
	}

	public boolean isEmpty() {
		return this.list.isEmpty();
	}

	public Iterator<Entity_> iterator() {
		return this.list.iterator();
	}

	public int lastIndexOf(Entity_ entity) {
		return this.list.lastIndexOf(entity);
	}

	public ListIterator<Entity_> listIterator() {
		return this.list.listIterator();
	}

	public int size() {
		return this.list.size();
	}

	public List<Entity_> subList(int arg0, int arg1) {
		return this.list.subList(arg0, arg1);
	}
	
	public String getCursor() {
		return this.cursor;
	}
	
	public boolean more() {
		return this.more;
	}
		
    @Override
    public int hashCode() {
        int hashCode = 23;
        hashCode = (hashCode * 37) + getClass().hashCode();
        hashCode = (hashCode * 37) + (this.list == null ? 1 : this.list.hashCode());
        hashCode = (hashCode * 37) + (this.cursor == null ? 1 : this.cursor.hashCode());
        hashCode = (hashCode * 37) + Boolean.valueOf(this.more).hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other.getClass().equals(this.getClass())) {
        	ListPage<?> o = (ListPage<?>) other;
            return true
                && ((o.list == null && this.list == null) || (o.list != null && o.list.equals(this.list)))
                && ((o.cursor == null && this.cursor == null) || (o.cursor != null && o.cursor.equals(this.cursor)))
                && o.more == this.more
               ;
        }
        return false;
    }

    @Override
    public String toString() {
        return "ListPage["
            + this.list
            + ","
            + this.cursor
            + ","
            + this.more
            + "]";
    }
}
