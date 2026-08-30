


class CMinMaxSLPT : public CTradingOption {
   private:
      
   public:
      CMinMaxSLPT() {
      }

      //+----------------------------------------------+

      virtual bool onBarUpdate() {
         return true;
      }
};

// create variable for class instance (required)
CMinMaxSLPT* objMinMaxSLPT;