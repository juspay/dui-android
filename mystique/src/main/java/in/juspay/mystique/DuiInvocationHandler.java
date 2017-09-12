package in.juspay.mystique;

/*
* Copyright (c) 2012-2017 "JUSPAY Technologies"
* JUSPAY Technologies Pvt. Ltd. [https://www.juspay.in]
*
* This file is part of JUSPAY Platform.
*
* JUSPAY Platform is free software: you can redistribute it and/or modify
* it for only educational purposes under the terms of the GNU Affero General
* Public License (GNU AGPL) as published by the Free Software Foundation,
* either version 3 of the License, or (at your option) any later version.
* For Enterprise/Commerical licenses, contact <info@juspay.in>.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  The end user will
* be liable for all damages without limitation, which is caused by the
* ABUSE of the LICENSED SOFTWARE and shall INDEMNIFY JUSPAY for such
* damages, claims, cost, including reasonable attorney fee claimed on Juspay.
* The end user has NO right to claim any indemnification based on its use
* of Licensed Software. See the GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/agpl.html>.
*/

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Created by sahebjot on 09/09/16.
 */
public class DuiInvocationHandler implements InvocationHandler {

    private Object obj;
    private boolean invokeOriginal = true;
    private DynamicUI dynamicUI;

    DuiInvocationHandler(Object obj, DynamicUI dynamicUI) {
        this.obj = obj;
        this.dynamicUI = dynamicUI;
    }

    public void setInvokeOriginal(boolean invokeOriginal) {
        this.invokeOriginal = invokeOriginal;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            if (invokeOriginal) {
                result = method.invoke(obj, args);
            }
            result = result == null ? "null" : result;
            String argsVal;
            if (args != null) {
                // , separated arguments, need to be split(',') js side
                argsVal = "\"";
                String append = ",";
                for (int i = 0; i < args.length; i++) {
                    if (i == args.length - 1) {
                        append = "";
                    }
                    argsVal = argsVal + args[i] + append;
                }
                argsVal = argsVal + "\"";
            } else {
                argsVal = "''";
            }
            dynamicUI.addJsToWebView("window.duiProxyCallback('" + result + "','" + obj.getClass().getName() + "','" + method.getName() + "'," + argsVal + ");");
        } catch (Exception e) {
            String methodDetails = this.obj.getClass().getName() + "-" + method.getName();
            dynamicUI.getErrorCallback().onError("InvocationHandler", e.getMessage() + "-" + methodDetails);
        }
        return result;
    }
}
