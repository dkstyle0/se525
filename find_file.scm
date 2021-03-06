(define (listProcessor mylist tv myfile)
  (if (> (.size mylist) 1)
      (begin (main (.getPath (.get mylist 0)) tv myfile)
             (.remove mylist 0)
             (listProcessor mylist tv myfile))
      (main (.getPath(.get mylist 0)) tv myfile)))

(define (main target tv myfile)
(let ((f (java.io.File. target)))
(if (not(.equals (.getName f) ".android_secure"))
(if (.isDirectory f)
    (begin (if (> (.size (java.util.ArrayList. (java.util.Arrays.asList(.list f)))) 0)
            (begin
             (let  ((alist (java.util.ArrayList. (java.util.Arrays.asList(.listFiles f)))))
            (main (.getPath (.get alist 0)) tv myfile)
            (.remove alist (.get alist 0))
            (if (> (.size alist) 0)
            (listProcessor alist tv myfile))))))
    (begin (if (.equals myfile (.getName f))
               (.setText tv  myfile))))))

(if (.equals (.getText tv) "")
   (.setText tv "not found")))


