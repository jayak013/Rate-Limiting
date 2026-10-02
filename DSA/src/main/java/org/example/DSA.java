package org.example;

import java.util.*;

public class DSA {
    static void main() {
        System.out.println(Arrays.toString(temperatures(new int[]{73,74,75,71,69,72,76,73})));
//        int[] nums = {1, 2, 3, 4, 5, 6, 7};
//        rotateArray(nums, 3);
//        IO.println(searchInRotatedArray(nums, 2));
//        IO.println(binarySearch(new int[]{1, 2, 3, 4}, 4));
//        IO.println(Arrays.toString(productOfArrayExceptItSelf(new int[]{1, 2, 3, 4})));
//        IO.println(longestSubStringWithoutRepeatingCharactersOptimized("geeksforgeeks"));
//        IO.println(Arrays.toString(twoSum(new int[]{2, 9, 7, 11, 15}, 9)));
    }

    private static int[] temperatures(int[] nums){
        Stack<Integer> stack = new Stack<>();
        int[] result = new int[nums.length];
        for(int i = 0; i < nums.length; i++){
            while(!stack.isEmpty() && nums[stack.peek()] < nums[i]){
                int prevIndex = stack.pop();
                result[prevIndex] = i - prevIndex;
            }
            stack.push(i);
        }
        return  result;
    }

    private static List<List<Integer>> threeSum(int[] nums){
        // a + b + c = 0
        // a + b = c
        // c - a = b

        // best approach
        Set<List<Integer>> resSet = new TreeSet<>((a, b)->{
            for(int i = 0; i < a.size(); i++){
                int comp = a.get(i) - b.get(i);
                if(comp!=0) return comp;
            }
            return 0;
        });

        for(int i = 0; i < nums.length; i++){
            Set<Integer> set = new HashSet<>();
            for(int j = i + 1; j < nums.length; j++){
                int complement = -nums[i] - nums[j];
                if(set.contains(complement)){
                    List<Integer> curr = new ArrayList<>(Arrays.asList(nums[i], nums[j], complement));
                    Collections.sort(curr);
                    resSet.add(curr);
                }
                set.add(nums[j]);
            }
        }

        List<List<Integer> > res = new ArrayList<>();

        for (List<Integer> triplet : resSet)
            res.add(new ArrayList<>(triplet));

        return res;

    }

    private static int[][] mergeIntervals(int[][] intervals){
        // [[1,7],[2,6],[8,10],[15,18]]
        // [[1,6],[8,10],[15,18]]
        Arrays.sort(intervals, Comparator.comparingInt(arr -> arr[0]));
        List<int[]> merged = new ArrayList<>();
        for(int[] interval:intervals){
            if(merged.isEmpty() || merged.getLast()[1] < interval[0]){
                merged.add(interval);
            }else{
                merged.getLast()[1] = Math.max(merged.getLast()[1], interval[1]);
            }
        }
        return merged.toArray(new int[merged.size()][]);
    }

    private static int searchInRotatedArray(int[] nums, int target){
        // [5, 6, 7, 1, 2, 3, 4] = target - 2
        int left = 0, right = nums.length - 1;
        while(left <= right){
            int mid = left + (right - left) / 2;
            if(nums[mid] == target) return mid;
            if(nums[left] <= nums[mid]){
                if(nums[left] <= target && target < nums[mid]) right = mid -1 ;
                else left = mid + 1;
            }else{
                if(nums[mid] < target && target <= nums[right]) left = mid + 1 ;
                else right = mid - 1;
            }
        }
        return -1;
    }

    private static void rotateArray(int[] nums, int k) {
        reverse(nums, 0, nums.length - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, nums.length - 1);
    }

    private static void reverse(int[] nums, int i, int j) {
        while (i < j) {
            int temp = nums[i];
            nums[i++] = nums[j];
            nums[j--] = temp;
        }
    }

    private static int binarySearch(int[] nums, int target) {
        int low = 0, high = nums.length - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) low = mid + 1;
            else high = mid - 1;
        }
        return -1;
    }

    private static int[] productOfArrayExceptItSelf(int[] nums) {
        int[] prefixSum = new int[nums.length];
        prefixSum[0] = 1;
        for (int i = 1; i < nums.length; i++) {
            prefixSum[i] = prefixSum[i - 1] * nums[i - 1];
        }

        int[] suffixSum = new int[nums.length];
        suffixSum[nums.length - 1] = 1;
        for (int i = nums.length - 2; i >= 0; i--) {
            suffixSum[i] = suffixSum[i + 1] * nums[i + 1];
        }

        for (int i = 0; i < nums.length; i++) {
            nums[i] = prefixSum[i] * suffixSum[i];
        }

        return nums;
    }

    private static String longestSubStringWithoutRepeatingCharactersOptimized(String str) {
        //pwwkew
        //wpkw
        String result = "";
        int i = 0, j = 0;
        Set<Character> set = new HashSet<>();
        while (j < str.length()) {
            while (set.contains(str.charAt(j))) {
                set.remove(str.charAt(i++));
            }
            set.add(str.charAt(j++));
            int window = j - i;
            if (window > result.length()) {
                result = str.substring(i, j);
            }
        }
        return result;
    }

    private static String longestSubStringWithoutRepeatingCharactersBruteForce(String str) {
        //pwwkew
        //wpkw
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            Set<Character> set = new HashSet<>();
            int j = i;
            StringBuilder sb = new StringBuilder();
            while (j < str.length() && !set.contains(str.charAt(j))) {
                sb.append(str.charAt(j));
                set.add(str.charAt(j++));
            }
            if (result.length() < sb.length()) {
                result = sb.toString();
            }
        }
        return result;
    }

    private static int[] twoSum(int[] nums, int target) {
        //target = 5
        // a = 3
        // b = 2
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(nums[i])) {
                return new int[]{map.get(nums[i]), i};
            } else {
                map.put(complement, i);
            }
        }
        return new int[]{-1, -1};
    }
}



