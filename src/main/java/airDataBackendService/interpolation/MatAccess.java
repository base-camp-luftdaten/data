package airDataBackendService.interpolation;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MatAccess 
{
	private double p1[][];
	private double p2[][];
	
	public double lat[][];
	public double lon[][];
	
	public MatAccess(File file)
	{
		double [][][] data = MatReader.read(file);
		double[][][] meshGrid = MatReader.meshGrid();
		p1=data[0];
		p2=data[1];
		
		lat=meshGrid[0];
		lon=meshGrid[1];
	}
	
	public MatAccess(InputStream str)
	{
		double [][][] data = MatReader.read(str);
		double[][][] meshGrid = MatReader.meshGrid();
		p1=data[0];
		p2=data[1];
		
		lat=meshGrid[0];
		lon=meshGrid[1];
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray pointArray(double lonMax, double latMax, double lonMin, double latMin, int posiblePoints, boolean finish, boolean P1P2)
	{
		JSONArray pList;
		if(lonMax<=16 && lonMin>=5 && latMax<=55 && latMin>=47)
		{
			int obererLatIndex = latToIndex(latMax);
			int untererLatIndex = latToIndex(latMin);
			int linkerLonIndex = lonToIndex(lonMin);
			int rechterLonIndex = lonToIndex(lonMax);
			
			int hight = (obererLatIndex-untererLatIndex+1);
			int width = (rechterLonIndex-linkerLonIndex+1);
			int fieldSize = hight*width;
			
			if(fieldSize<=posiblePoints)
			{
				pList= new JSONArray();
				double val;
				for(int i=0;i<=hight;i++)
				{
					for(int j=0;j<=width;j++)
					{
						if(Double.isFinite(p1[i+untererLatIndex][j+linkerLonIndex])&&Double.isFinite(p2[i+untererLatIndex][j+linkerLonIndex]))
						{
							
							if(P1P2==false)
							{
								val=p1[i+untererLatIndex][j+linkerLonIndex];
							}
							else
							{
								val=p2[i+untererLatIndex][j+linkerLonIndex];
							}
							JSONObject point = createHeatmapPoint(
									lon[i+untererLatIndex][j+linkerLonIndex], 
									lat[i+untererLatIndex][j+linkerLonIndex],
									val);
							
							pList.add(point);
						}
					}
				}
				return pList;
			}
			else
			{
				int reduce = (int)(Math.ceil(((double)fieldSize)/((double)posiblePoints)));
				int[] dist = bestDistance(reduce);
				pList= new JSONArray();
				double val;
				
				for(int i=0;i<=hight;i=i+dist[0])
				{
					int h = i;
					
					for(int j=0;j<=width;j=j+(dist[1]*2))
					{
						if(Double.isFinite(p1[h+untererLatIndex][j+linkerLonIndex])&&Double.isFinite(p2[h+untererLatIndex][j+linkerLonIndex]))
						{
							
							if(P1P2==false)
							{
								val=p1[h+untererLatIndex][j+linkerLonIndex];
							}
							else
							{
								val=p2[h+untererLatIndex][j+linkerLonIndex];
							}
							JSONObject point = createHeatmapPoint(
									lon[h+untererLatIndex][j+linkerLonIndex], 
									lat[h+untererLatIndex][j+linkerLonIndex],
									val);
							
							pList.add(point);
							
						}
					}
					
					h=h+(dist[0]/2);
					if(h<=hight)
					{
						for(int j=dist[1];j<=width;j=j+(dist[1]*2))
						{
							if(Double.isFinite(p1[h+untererLatIndex][j+linkerLonIndex])&&Double.isFinite(p2[h+untererLatIndex][j+linkerLonIndex]))
							{
								if(P1P2==false)
								{
									val=p1[h+untererLatIndex][j+linkerLonIndex];
								}
								else
								{
									val=p2[h+untererLatIndex][j+linkerLonIndex];
								}
								JSONObject point = createHeatmapPoint(
										lon[h+untererLatIndex][j+linkerLonIndex], 
										lat[h+untererLatIndex][j+linkerLonIndex],
										val);
								
								pList.add(point);
								
							}
						}
					}
					
					if(finish==true)
					{
						if(Double.isFinite(p1[i+untererLatIndex][rechterLonIndex])&&Double.isFinite(p2[h+untererLatIndex][rechterLonIndex]))
						{
							if(P1P2==false)
							{
								val=p1[i+untererLatIndex][rechterLonIndex];
							}
							else
							{
								val=p2[i+untererLatIndex][rechterLonIndex];
							}
							JSONObject point = createHeatmapPoint(
									lon[i+untererLatIndex][rechterLonIndex], 
									lat[i+untererLatIndex][rechterLonIndex],
									val);
							
							pList.add(point);
							
						}
					}
				}
				if(finish==true)
				{
					for(int j=0;j<=width;j=j+(dist[1]*2))
					{
						if(Double.isFinite(p1[obererLatIndex][j+linkerLonIndex])&&Double.isFinite(p2[obererLatIndex][j+linkerLonIndex]))
						{
							if(P1P2==false)
							{
								val=p1[obererLatIndex][j+linkerLonIndex];
							}
							else
							{
								val=p2[obererLatIndex][j+linkerLonIndex];
							}
							JSONObject point = createHeatmapPoint(
									lon[obererLatIndex][j+linkerLonIndex], 
									lat[obererLatIndex][j+linkerLonIndex],
									val);
							
							pList.add(point);
							
						}
					}
					
					if(Double.isFinite(p1[obererLatIndex][rechterLonIndex])&&Double.isFinite(p2[obererLatIndex][rechterLonIndex]))
					{
						if(P1P2==false)
						{
							val=p1[obererLatIndex][rechterLonIndex];
						}
						else
						{
							val=p2[obererLatIndex][rechterLonIndex];
						}
						JSONObject point = createHeatmapPoint(
								lon[obererLatIndex][rechterLonIndex], 
								lat[obererLatIndex][rechterLonIndex],
								val);
						
						pList.add(point);
						
					}
				}
				return pList;
			}
		}
		else
		{
			System.out.println("Außerhalb des Interpolations-Radius");
			return null;
		}
		
	}
	
	private int latToIndex(double i)
	{
		return (int) ((i-47.0)/0.002);
	}
	
	public int lonToIndex(double i)
	{
		return (int) ((i-5.0)/0.004);
	}
	
	private int[] bestDistance(int n)
	{
		int nextSquare=(int)(Math.ceil(Math.sqrt(n)));
		
		if(((nextSquare-1)*nextSquare)>=n)
		{			
			if(nextSquare%2==0)
			{
				return new int[]{nextSquare,(nextSquare-1)};
			}
			else
			{
				return new int[]{(nextSquare-1),nextSquare};
			}
		}
		else if(nextSquare%2==0)
		{
			return new int[]{nextSquare,nextSquare};
		}
		else
		{
			return new int[]{(nextSquare+1),nextSquare};
		}
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject createHeatmapPoint(double lon, double lat, double val)
	{
		JSONObject point = new JSONObject();
		point.put("x", lon);
		point.put("y", lat);
		point.put("value", val);
		return point;
	}
}
