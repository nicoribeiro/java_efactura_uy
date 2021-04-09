package com.bluedot.efactura.strategy.asignarFecha;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.bluedot.commons.utils.DateHandler;

public class EstrategiaFechaRango implements EstrategiaAsignarFecha {

	private int origin;
	private int bound;
	boolean before;
	List<Integer> randomNumbers;
	int index = 0;
	
	public EstrategiaFechaRango(int origin, int bound, boolean before) {
		super();
		this.origin = origin;
		this.bound = bound;
		this.before = before;
		Random random = new Random();
		randomNumbers = random.ints(origin, bound).distinct().limit(bound-origin).boxed().collect(Collectors.toList());
	}

	@Override
	public Date getDate() {
		if (index == randomNumbers.size())
			index=0;
				
		if (before)
			return DateHandler.minus(new Date(), randomNumbers.get(index++), Calendar.DAY_OF_MONTH);
		else
			return DateHandler.add(new Date(), randomNumbers.get(index++), Calendar.DAY_OF_MONTH);
	}

	

}