package com.hankcs.textrank;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.ToAnalysis;

import com.sun.corba.se.spi.orbutil.fsm.Guard.Result;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.print.DocFlavor.STRING;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * TextRank关键词提取
 * @author hankcs
 */
public class TextRankKeyword
{
    public static int nKeyword = 15;
    StopWordDictionary StopWordDict = new StopWordDictionary();
    /**
     * 阻尼系数（ＤａｍｐｉｎｇＦａｃｔｏｒ），一般取值为0.85
     */
    static final float d = 0.85f;
    /**
     * 最大迭代次数
     */
    static final int max_iter = 200;
    static final float min_diff = 0.001f;

    public TextRankKeyword()
    {
        // jdk bug : Exception in thread "main" java.lang.IllegalArgumentException: Comparison method violates its general contract!
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    public List<Term> getTerms(String file){
		List<Term> terms = new ArrayList<Term>();
        StringBuffer strSb = new StringBuffer(); //String is constant， StringBuffer can be changed.
        try{
	        InputStreamReader inStrR = new InputStreamReader(new FileInputStream(file), "utf-8"); //byte streams to character streams
	        BufferedReader br = new BufferedReader(inStrR); 
	        String line = br.readLine();
	        String[] spilts = line.split("\t");
	        for (String it : spilts) {
	        	terms.add(new Term(it, 2, null));
			}
	        inStrR.close();
       	}catch(Exception e){}
		return terms;
	}
    public List<Term> getTermsFormAnalysis(String filename) {
		List<Term> terms = new ArrayList<Term>();
        StringBuffer strSb = new StringBuffer(); //String is constant， StringBuffer can be changed.
        try{
	        InputStreamReader inStrR = new InputStreamReader(new FileInputStream(filename), "gbk"); //byte streams to character streams
	        BufferedReader br = new BufferedReader(inStrR);
	        String line = br.readLine();
	        List<Term> termList;
	        while (line!=null) {				  
	        	termList = ToAnalysis.parse(line);
	        	terms.addAll(termList);
//	        	System.out.println(terms);
	        	line = br.readLine();
			}
       	}catch(Exception e){}
		return terms;
	}
    public List<String> getKeys(String file){
		List<String> terms = new ArrayList<String>();
        StringBuffer strSb = new StringBuffer(); //String is constant， StringBuffer can be changed.
        try{
	        InputStreamReader inStrR = new InputStreamReader(new FileInputStream(file), "utf-8"); //byte streams to character streams
	        BufferedReader br = new BufferedReader(inStrR); 
	        String line = br.readLine();
	        String[] spilts = line.split("\t");
	        for (String it : spilts) {
	        	terms.add(it);
			}
	        inStrR.close();
       	}catch(Exception e){}
		return terms;
	}
    public List<String> getKeyword(String filename)
    {
//        List<Term> termList = ToAnalysis.parse(content);
//    	List<Term> termList = getTerms(filename);
    	List<Term> termList = getTermsFormAnalysis(filename);
        List<String> wordList = new ArrayList<String>();
        for (Term t : termList)
        {
            if (shouldInclude(t))
            {
                wordList.add(t.getName());
            }
        }
//        System.out.println(wordList);
        Map<String, Set<String>> words = new HashMap<String, Set<String>>();
        Queue<String> que = new LinkedList<String>();
        for (String w : wordList)
        {
            if (!words.containsKey(w))
            {
                words.put(w, new HashSet<String>());
            }
            que.offer(w);
            if (que.size() > 5)
            {
                que.poll();
            }

            for (String w1 : que)
            {
                for (String w2 : que)
                {
                    if (w1.equals(w2))
                    {
                        continue;
                    }

                    words.get(w1).add(w2);
                    words.get(w2).add(w1);
                }
            }
        }
//        System.out.println(words);
        Map<String, Float> score = new HashMap<String, Float>();
        for (int i = 0; i < max_iter; ++i)
        {
            Map<String, Float> m = new HashMap<String, Float>();
            float max_diff = 0;
            for (Map.Entry<String, Set<String>> entry : words.entrySet())
            {
                String key = entry.getKey();
                Set<String> value = entry.getValue();
                m.put(key, 1 - d);
                for (String other : value)
                {
                    int size = words.get(other).size();
                    if (key.equals(other) || size == 0) continue;
                    m.put(key, m.get(key) + d / size * (score.get(other) == null ? 0 : score.get(other)));
                }
                max_diff = Math.max(max_diff, Math.abs(m.get(key) - (score.get(key) == null ? 0 : score.get(key))));
            }
            score = m;
            if (max_diff <= min_diff) break;
        }
        List<Map.Entry<String, Float>> entryList = new ArrayList<Map.Entry<String, Float>>(score.entrySet());
        Collections.sort(entryList, new Comparator<Map.Entry<String, Float>>()
        {
            @Override
            public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2)
            {
                return (o1.getValue() - o2.getValue() > 0 ? -1 : 1);
            }
        });
//        System.out.println(entryList);
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < nKeyword; ++i)
        {
        	if (i >= entryList.size()) {
				break;
			}
//            result += entryList.get(i).getKey() + '\t' + entryList.get(i).getValue()+"\n";
        	result.add(entryList.get(i).getKey());
        }
        return result;
    }
    public String PriciseAndRecall(List<String> terms,List<String> keys) {
		double pricise = 0.0, recall = 0.0 , keys_len = (double)(keys.size()) ,terms_len = (double)(terms.size()) , right_count = 0.0, b = 1.0, f_measure = 0.0;
		
		for (String it : terms) {
			if (keys.contains(it)) {
				right_count +=1.0;
			}
		}
		
		pricise = right_count/keys_len;
		recall = right_count / terms_len;
		f_measure = (b*b + 1) * pricise * recall / (b*b*pricise + recall);
		
		String pricise_string = String.format("精确率 ：\t%.4f\n", pricise);
		String recall_string = String.format("召回率 ：\t%.4f\n", recall);
		String f_measure_string = String.format("f-measure ：\t%.4f\n", f_measure);
		
		String result = "\nTextRank\n";
		result += pricise_string + recall_string + f_measure_string;
		return result;
	}
    //get list of file for the directory, including sub-directory of it
    public List<String> getDirFiles(String filepath)
    {
    	List<String> fileList = new ArrayList<String>(); 
        try
        {
            File file = new File(filepath);
            if(file.isDirectory())
            {
                String[] flist = file.list();
                for(int i = 0; i < flist.length; i++)
                {
                    File newfile = new File(filepath + "\\" + flist[i]);
                    if(!newfile.isDirectory())
                    {
                    	fileList.add(newfile.getName()); // .getAbsolutePath()
                    }
                }
            }
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
        return fileList;
    }
    //write file
    public  void writeToFile(String dir, String file,String content)
    {
       StringBuffer strSb = new StringBuffer(); //String is constant， StringBuffer can be changed.
       File f = new File(file);
       File path =new File(dir);
       File dirs=new File(path, f.getName());
       try{
	       if(!dirs.exists()){
	    	   dirs.createNewFile(); 
		   }
	        FileOutputStream FILE = new FileOutputStream(dir + "\\" + f.getName());        
	        OutputStreamWriter outStrW = new OutputStreamWriter(FILE, "utf-8"); //byte streams to character streams
	
	        outStrW.write(content);
	        outStrW.flush();
	
	        outStrW.close();
       }catch(Exception e){}
       
    }
    //    TextRank
    public void AutoTextRank(String origin_dir, String key_dir, String stat_dir) {
		List<String> filesList = getDirFiles(origin_dir);
		TextRankKeyword textRankKeyword = new TextRankKeyword();
		
		for (String filename : filesList) {
	        List<String> terms = textRankKeyword.getKeyword(origin_dir + filename);	        
	        System.out.println(origin_dir + filename);	        
	        System.out.println(terms);
	        
	        List<String> keys = textRankKeyword.getKeys(key_dir + filename);
	        System.out.println(key_dir + filename);
	        System.out.println(keys);
	        
	        String result = textRankKeyword.PriciseAndRecall(terms, keys);
	        System.out.print(result);
	        
	        String content = String.format("%s\n%s\n", origin_dir + filename, terms.toString());
	        content += String.format("%s\n%s\n", key_dir + filename, keys.toString());
	        content += String.format("%s\n", result);
	        
	        writeToFile(stat_dir, filename, content);
		}
	}
    public static void main(String[] args)
    {
//        String origin_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\origin\\";
    	String origin_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\corpus\\C5-Education\\";
        String spilt_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\spilt\\";
        String key_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\key\\";
        String stat_dir = "./stat/";
        TextRankKeyword textRankKeyword = new TextRankKeyword();
        textRankKeyword.AutoTextRank(origin_dir, key_dir, stat_dir);
    }
    
    public void test() {
    	String filename = "file.txt";
        String origin_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\origin\\";
        String spilt_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\spilt\\";
        String key_dir = "E:\\ItemForGo\\src\\github.com\\shaalx\\sstruct\\static\\key\\";
        TextRankKeyword textRankKeyword = new TextRankKeyword();
        
//      List<String> terms = textRankKeyword.getKeyword(spilt_dir + filename);
//      System.out.println(spilt_dir + filename);        

        List<String> terms = textRankKeyword.getKeyword(origin_dir + filename);        
        System.out.println(origin_dir + filename);
        
        System.out.println(terms);
        
        List<String> keys = textRankKeyword.getKeys(key_dir + filename);
        System.out.println(key_dir + filename);
        System.out.println(keys);
        
        String result = textRankKeyword.PriciseAndRecall(terms, keys);
        System.out.print(result);
	}
    /**
     * 是否应当将这个term纳入计算，词性属于名词、动词、副词、形容词
     * @param term
     * @return 是否应当
     */
    public boolean shouldInclude(Term term)
    {
//        if (
//                term.getNatrue().natureStr.startsWith("n") ||
//                term.getNatrue().natureStr.startsWith("v") ||
//                term.getNatrue().natureStr.startsWith("d") ||
//                term.getNatrue().natureStr.startsWith("a")
//                )
//        {
//            // TODO 你需要自己实现一个停用词表
////            if (!StopWordDictionary.contains(term.getName()))
////            {
//                return true;
////            }
//        }
      if (!StopWordDict.contains(term.getName()))
      {
          return true;
      }
      return false;
    }
    class StopWordDictionary{
    	List<String> dict = new ArrayList<String>();
    	public StopWordDictionary(){
    		dict.add("的");dict.add("在");dict.add("是");dict.add("与");dict.add("了");
    		dict.add("和");dict.add("一");dict.add("中");dict.add("这");dict.add("使");
    		dict.add("上");dict.add("从");dict.add("不");dict.add("也");dict.add("其");
    		dict.add("对");dict.add("没有");dict.add("这么");dict.add("这些");dict.add("一个");
    		dict.add("将");dict.add("而");dict.add("地");dict.add("他");dict.add("她");dict.add("它");
    		dict.add("会");dict.add("你");dict.add("我");dict.add("这个");
    	}
    	public boolean contains(String termname) {
    		return dict.contains(termname) || 1 >= termname.length();
		}
    }
}
