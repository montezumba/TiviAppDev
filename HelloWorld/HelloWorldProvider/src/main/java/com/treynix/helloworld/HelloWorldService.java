package com.treynix.helloworld;

import com.treynix.tiviapplive.provider.android.AddonService;

public class HelloWorldService extends AddonService {

	public HelloWorldService() {

		super(HelloWorldConfig.class, HelloWorldMain.class);
	}

}
